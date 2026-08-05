package com.academy.trafficviolationsystem.notification;

import com.academy.trafficviolationsystem.appeal.ViolationAppealEntity;
import com.academy.trafficviolationsystem.camera.CameraEntity;
import com.academy.trafficviolationsystem.core.services.BaseService;
import com.academy.trafficviolationsystem.driver.DriverEntity;
import com.academy.trafficviolationsystem.driver.LicenseSuspensionEntity;
import com.academy.trafficviolationsystem.fine.FineEntity;
import com.academy.trafficviolationsystem.payment.PaymentEntity;
import com.academy.trafficviolationsystem.user.UserEntity;
import com.academy.trafficviolationsystem.user.UserRepository;
import com.academy.trafficviolationsystem.user.UserRole;
import com.academy.trafficviolationsystem.vehicle.VehicleEntity;
import jakarta.persistence.EntityManager;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.repository.CrudRepository;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * Central notification dispatcher.
 *
 * All other services call this service — never EmailNotificationService,
 * SmsNotificationService, or a NotificationSender directly. This keeps the
 * notification channel implementation decoupled from the domain modules.
 *
 * Channel dispatch (dispatchRaw) uses the Bridge pattern: NotificationSender
 * is the implementor interface, with EmailNotificationSender /
 * SmsNotificationSender / InAppNotificationSender as concrete implementors.
 * NotificationSenderFactory (Factory Method) resolves the right one for a
 * given NotificationType, so this class never references a concrete channel
 * by name.
 *
 * Every public send* method is annotated @Async("notificationExecutor") so
 * it runs on the dedicated notification thread pool without blocking the
 * HTTP request thread that triggered the domain operation.
 *
 * Write-first pattern:
 *   NotificationEntity is persisted with status=PENDING before dispatching.
 *   This means even if the SMTP/SMS call fails, the notification row exists
 *   and NotificationRetryJob can retry it later.
 *
 * Retry logic (called by NotificationRetryJob):
 *   retryCount 0 → 1:  nextRetryAt = now + 5 min,  status = RETRYING
 *   retryCount 1 → 2:  nextRetryAt = now + 15 min, status = RETRYING
 *   retryCount 2 → 3:  nextRetryAt = now + 60 min, status = RETRYING
 *   retryCount >= 3:    status = FAILED (permanent)
 *
 * Template resolution:
 *   Templates are looked up by templateKey + NotificationType + language.
 *   Falls back to language="bs" if the preferred language has no template.
 *   Falls back to sending raw body (no template) if no template exists at all.
 *
 * Implements BaseService for the read-only search/findById endpoints.
 */
@Service
@Transactional
public class NotificationService implements BaseService<
        NotificationEntity, NotificationDto, NotificationSearchObject, UUID> {

    private static final Logger log = LoggerFactory.getLogger(NotificationService.class);

    private static final int    MAX_RETRIES         = 3;
    private static final String DEFAULT_LANGUAGE    = "bs";
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd.MM.yyyy");

    private final NotificationRepository         notificationRepository;
    private final NotificationTemplateRepository templateRepository;
    private final NotificationMapper             notificationMapper;
    private final TemplateRenderer               templateRenderer;
    private final NotificationSenderFactory      notificationSenderFactory;
    private final UserRepository                 userRepository;
    private final EntityManager                  entityManager;

    public NotificationService(NotificationRepository notificationRepository,
                               NotificationTemplateRepository templateRepository,
                               NotificationMapper notificationMapper,
                               TemplateRenderer templateRenderer,
                               NotificationSenderFactory notificationSenderFactory,
                               UserRepository userRepository,
                               EntityManager entityManager) {
        this.notificationRepository   = notificationRepository;
        this.templateRepository       = templateRepository;
        this.notificationMapper       = notificationMapper;
        this.templateRenderer         = templateRenderer;
        this.notificationSenderFactory = notificationSenderFactory;
        this.userRepository           = userRepository;
        this.entityManager            = entityManager;
    }

    // ── BaseService wiring ────────────────────────────────────────────────

    @Override public CrudRepository<NotificationEntity, UUID> getRepository()    { return notificationRepository; }
    @Override public EntityManager                            getEntityManager() { return entityManager;          }
    @Override public NotificationMapper                       getMapper()        { return notificationMapper;     }
    @Override public Class<NotificationEntity>                getEntityClass()   { return NotificationEntity.class; }

    // ── search ────────────────────────────────────────────────────────────

    @Override
    public List<Predicate> additionalFilter(CriteriaBuilder cb,
                                            NotificationSearchObject searchObj,
                                            Root<NotificationEntity> root) {
        List<Predicate> predicates = new ArrayList<>();

        if (searchObj.getStatus() != null) {
            predicates.add(cb.equal(root.get("status"), searchObj.getStatus()));
        }
        if (searchObj.getType() != null) {
            predicates.add(cb.equal(root.get("type"), searchObj.getType()));
        }
        if (searchObj.getUserId() != null) {
            predicates.add(cb.equal(root.get("user").get("id"), searchObj.getUserId()));
        }
        if (searchObj.getRelatedEntityId() != null) {
            predicates.add(cb.equal(root.get("relatedEntityId"), searchObj.getRelatedEntityId()));
        }
        if (searchObj.getRelatedEntityType() != null) {
            predicates.add(cb.equal(root.get("relatedEntityType"), searchObj.getRelatedEntityType()));
        }
        if (searchObj.getFromDate() != null) {
            predicates.add(cb.greaterThanOrEqualTo(
                    root.get("created"), searchObj.getFromDate().atStartOfDay()));
        }
        if (searchObj.getToDate() != null) {
            predicates.add(cb.lessThan(
                    root.get("created"), searchObj.getToDate().plusDays(1).atStartOfDay()));
        }
        return predicates;
    }

    // ── domain send methods ───────────────────────────────────────────────

    /**
     * Sent by FineService.onViolationConfirmed() after a fine is issued.
     * Dispatches both EMAIL and SMS if the driver has both contact details.
     */
    @Async("notificationExecutor")
    public void sendFineIssuedNotification(FineEntity fine) {
        if (fine.getDriver() == null) return;
        DriverEntity driver = fine.getDriver();

        Map<String, String> vars = new HashMap<>();
        vars.put("driverName",   driver.getFirstName() + " " + driver.getLastName());
        vars.put("fineNumber",   fine.getFineNumber());
        vars.put("amount",       fine.getCurrency() + " " + fine.getTotalDue());
        vars.put("dueDate",      fine.getDueDate() != null ? fine.getDueDate().format(DATE_FMT) : "-");
        vars.put("violationRef", ""); // populated lazily by caller if needed

        UserEntity user = driver.getUser();
        UUID relatedId  = fine.getId();

        if (driver.getEmail() != null) {
            dispatch("FINE_ISSUED", NotificationType.EMAIL,
                    driver.getEmail(), vars, user, relatedId, "FINE");
        }
        if (driver.getPhoneNumber() != null) {
            dispatch("FINE_ISSUED", NotificationType.SMS,
                    driver.getPhoneNumber(), vars, user, relatedId, "FINE");
        }
    }

    /**
     * Sent by PaymentService.afterSuccessfulPayment().
     */
    @Async("notificationExecutor")
    public void sendPaymentConfirmedNotification(PaymentEntity payment, FineEntity fine) {
        if (fine.getDriver() == null) return;
        DriverEntity driver = fine.getDriver();

        Map<String, String> vars = new HashMap<>();
        vars.put("driverName",     driver.getFirstName() + " " + driver.getLastName());
        vars.put("fineNumber",     fine.getFineNumber());
        vars.put("amount",         payment.getCurrency() + " " + payment.getAmount());
        vars.put("transactionId",  payment.getTransactionId());
        vars.put("paidAt",         payment.getPaidAt() != null
                ? payment.getPaidAt().format(DATE_FMT) : "-");

        UserEntity user = driver.getUser();
        UUID relatedId  = payment.getId();

        if (driver.getEmail() != null) {
            dispatch("PAYMENT_SUCCESS", NotificationType.EMAIL,
                    driver.getEmail(), vars, user, relatedId, "PAYMENT");
        }
        if (driver.getPhoneNumber() != null) {
            dispatch("PAYMENT_SUCCESS", NotificationType.SMS,
                    driver.getPhoneNumber(), vars, user, relatedId, "PAYMENT");
        }
    }

    @Async("notificationExecutor")
    public void sendStolenVehicleAlert(VehicleEntity vehicle, CameraEntity camera) {
        Map<String, String> vars = new HashMap<>();
        vars.put("plate",          vehicle.getLicensePlate());
        vars.put("cameraSerial",   camera.getSerialNumber());
        vars.put("cameraLocation", camera.getLocationDescription());
        vars.put("detectedAt",     LocalDateTime.now().format(DATE_FMT));

        List<UserEntity> recipients =
                userRepository.findByRoleInAndIsActiveTrue(List.of(UserRole.OFFICER, UserRole.ADMIN));

        for (UserEntity recipient : recipients) {
            if (recipient.getEmail() != null) {
                dispatch("STOLEN_VEHICLE_ALERT", NotificationType.EMAIL,
                        recipient.getEmail(), vars, recipient, vehicle.getId(), "VEHICLE");
            }
        }
    }

    /**
     * Sent by AppealService.approve() and AppealService.reject().
     */
    @Async("notificationExecutor")
    public void sendAppealDecisionNotification(ViolationAppealEntity appeal) {
        if (appeal.getDriver() == null) return;
        DriverEntity driver = appeal.getDriver();

        Map<String, String> vars = new HashMap<>();
        vars.put("driverName",    driver.getFirstName() + " " + driver.getLastName());
        vars.put("appealNumber",  appeal.getAppealNumber());
        vars.put("decision",      appeal.getStatus().name());
        vars.put("reviewNotes",   appeal.getReviewNotes() != null ? appeal.getReviewNotes() : "");

        String templateKey = appeal.getStatus().name().equals("APPROVED")
                ? "APPEAL_APPROVED" : "APPEAL_REJECTED";

        UserEntity user = driver.getUser();
        UUID relatedId  = appeal.getId();

        if (driver.getEmail() != null) {
            dispatch(templateKey, NotificationType.EMAIL,
                    driver.getEmail(), vars, user, relatedId, "APPEAL");
        }
        if (driver.getPhoneNumber() != null) {
            dispatch(templateKey, NotificationType.SMS,
                    driver.getPhoneNumber(), vars, user, relatedId, "APPEAL");
        }
    }

    /**
     * Sent by DriverService.suspendInternal() when a driver is suspended.
     */
    @Async("notificationExecutor")
    public void sendSuspensionNotification(DriverEntity driver,
                                           LicenseSuspensionEntity suspension) {
        Map<String, String> vars = new HashMap<>();
        vars.put("driverName",     driver.getFirstName() + " " + driver.getLastName());
        vars.put("licenseNumber",  driver.getLicenseNumber());
        vars.put("reason",         suspension.getReason());
        vars.put("startDate",      suspension.getStartDate().format(DATE_FMT));
        vars.put("suspendedUntil", suspension.getEndDate() != null
                ? suspension.getEndDate().format(DATE_FMT) : "Indefinite");
        vars.put("penaltyPoints",  String.valueOf(suspension.getPointsAtTime()));

        UserEntity user = driver.getUser();
        UUID relatedId  = suspension.getId();

        if (driver.getEmail() != null) {
            dispatch("SUSPENSION_NOTICE", NotificationType.EMAIL,
                    driver.getEmail(), vars, user, relatedId, "SUSPENSION");
        }
        if (driver.getPhoneNumber() != null) {
            dispatch("SUSPENSION_NOTICE", NotificationType.SMS,
                    driver.getPhoneNumber(), vars, user, relatedId, "SUSPENSION");
        }
    }

    /**
     * Sent by LicensePointResetJob for drivers whose license expires within 30 days.
     */
    @Async("notificationExecutor")
    public void sendLicenseExpiryWarning(DriverEntity driver) {
        Map<String, String> vars = new HashMap<>();
        vars.put("driverName",    driver.getFirstName() + " " + driver.getLastName());
        vars.put("licenseNumber", driver.getLicenseNumber());
        vars.put("expiresAt",     driver.getLicenseExpiresAt().format(DATE_FMT));

        UserEntity user = driver.getUser();

        if (driver.getEmail() != null) {
            dispatch("LICENSE_EXPIRY_WARNING", NotificationType.EMAIL,
                    driver.getEmail(), vars, user, null, null);
        }
        if (driver.getPhoneNumber() != null) {
            dispatch("LICENSE_EXPIRY_WARNING", NotificationType.SMS,
                    driver.getPhoneNumber(), vars, user, null, null);
        }
    }

    /**
     * Sent by AuthService when an account is locked after too many failed logins.
     */
    @Async("notificationExecutor")
    public void sendAccountLockedNotification(UserEntity user) {
        if (user.getEmail() == null) return;

        Map<String, String> vars = new HashMap<>();
        vars.put("driverName", user.getFirstName() + " " + user.getLastName());

        dispatch("ACCOUNT_LOCKED", NotificationType.EMAIL,
                user.getEmail(), vars, user, null, null);
    }

    // ── retry (called by NotificationRetryJob) ────────────────────────────

    /**
     * Finds all RETRYING notifications whose nextRetryAt has elapsed and re-dispatches them.
     *
     * @return Number of notifications successfully re-dispatched.
     */
    @Transactional
    public int retryFailed() {
        List<NotificationEntity> due = notificationRepository.findDueForRetry(LocalDateTime.now());
        int successCount = 0;

        for (NotificationEntity notification : due) {
            boolean sent = dispatchRaw(notification);
            if (sent) {
                notificationRepository.markSent(notification.getId(), LocalDateTime.now());
                successCount++;
            } else {
                scheduleNextRetry(notification);
            }
        }

        return successCount;
    }

    // ── private: core dispatch ────────────────────────────────────────────

    /**
     * Core internal dispatch method.
     * 1. Resolves the template (with language fallback).
     * 2. Renders subject + body using TemplateRenderer.
     * 3. Persists NotificationEntity (PENDING) — write-first.
     * 4. Dispatches via email or SMS service.
     * 5. Updates status to SENT or schedules retry.
     */
    private void dispatch(String templateKey,
                          NotificationType type,
                          String recipient,
                          Map<String, String> variables,
                          UserEntity user,
                          UUID relatedEntityId,
                          String relatedEntityType) {
        try {
            // Resolve template
            NotificationTemplateEntity template = resolveTemplate(templateKey, type);

            String subject = template != null
                    ? templateRenderer.render(template.getSubject(), variables)
                    : null;
            String body = template != null
                    ? templateRenderer.render(template.getBodyTemplate(), variables)
                    : buildFallbackBody(templateKey, variables);

            // Persist before dispatching
            NotificationEntity notification = buildNotificationEntity(
                    type, subject, body, recipient, user, relatedEntityId, relatedEntityType);
            notification = notificationRepository.save(notification);

            // Dispatch
            boolean sent = dispatchRaw(notification);
            if (sent) {
                notificationRepository.markSent(notification.getId(), LocalDateTime.now());
            } else {
                scheduleNextRetry(notification);
            }

        } catch (Exception e) {
            log.error("Failed to dispatch {} notification to {}: {}", type, recipient, e.getMessage());
        }
    }

    /**
     * Resolves the right channel (Bridge) via NotificationSenderFactory
     * (Factory Method) and dispatches through it.
     * Returns true on success, false on failure.
     */
    private boolean dispatchRaw(NotificationEntity notification) {
        NotificationSender sender = notificationSenderFactory.getSender(notification.getType());
        return sender.send(
                notification.getRecipient(),
                notification.getSubject(),
                notification.getBody());
    }

    /**
     * Exponential backoff retry scheduling.
     * retryCount 0→1: +5 min, 1→2: +15 min, 2→3: +60 min, >=3: FAILED permanently.
     */
    private void scheduleNextRetry(NotificationEntity notification) {
        int nextCount = notification.getRetryCount() + 1;

        if (nextCount > MAX_RETRIES) {
            notificationRepository.markFailedWithRetry(
                    notification.getId(), "FAILED",
                    "Max retries (" + MAX_RETRIES + ") exhausted",
                    nextCount, null);
            return;
        }

        long minutesDelay = switch (nextCount) {
            case 1 -> 5;
            case 2 -> 15;
            default -> 60;
        };

        LocalDateTime nextRetryAt = LocalDateTime.now().plusMinutes(minutesDelay);
        notificationRepository.markFailedWithRetry(
                notification.getId(), "RETRYING",
                "Dispatch failed — retry scheduled in " + minutesDelay + " minutes",
                nextCount, nextRetryAt);
    }

    private NotificationTemplateEntity resolveTemplate(String templateKey, NotificationType type) {
        return templateRepository
                .findByTemplateKeyAndTypeAndLanguageAndIsActiveTrue(templateKey, type, DEFAULT_LANGUAGE)
                .or(() -> templateRepository.findFirstByTemplateKeyAndTypeAndIsActiveTrue(templateKey, type))
                .orElse(null);
    }

    private String buildFallbackBody(String templateKey, Map<String, String> variables) {
        // Minimal fallback when no template exists — should not happen if Flyway seed runs correctly
        StringBuilder sb = new StringBuilder("Notification: ").append(templateKey).append("\n");
        variables.forEach((k, v) -> sb.append(k).append(": ").append(v).append("\n"));
        return sb.toString();
    }

    private NotificationEntity buildNotificationEntity(NotificationType type,
                                                       String subject,
                                                       String body,
                                                       String recipient,
                                                       UserEntity user,
                                                       UUID relatedEntityId,
                                                       String relatedEntityType) {
        NotificationEntity n = new NotificationEntity();
        n.setType(type);
        n.setSubject(subject);
        n.setBody(body);
        n.setRecipient(recipient);
        n.setUser(user);
        n.setStatus(NotificationStatus.PENDING);
        n.setRetryCount(0);
        n.setRelatedEntityId(relatedEntityId);
        n.setRelatedEntityType(relatedEntityType);
        return n;
    }
}