package com.academy.trafficviolationsystem.appeal;

import com.academy.trafficviolationsystem.appeal.statemachine.AppealState;
import com.academy.trafficviolationsystem.appeal.statemachine.AppealStates;
import com.academy.trafficviolationsystem.audit.AuditAction;
import com.academy.trafficviolationsystem.core.exceptions.BadRequestException;
import com.academy.trafficviolationsystem.core.exceptions.NotFoundException;
import com.academy.trafficviolationsystem.core.exceptions.appeal.*;
import com.academy.trafficviolationsystem.core.security.UserPrincipal;
import com.academy.trafficviolationsystem.core.services.BaseCRUDService;
import com.academy.trafficviolationsystem.driver.DriverEntity;
import com.academy.trafficviolationsystem.driver.DriverRepository;
import com.academy.trafficviolationsystem.driver.DriverService;
import com.academy.trafficviolationsystem.fine.FineRepository;
import com.academy.trafficviolationsystem.fine.FineService;
import com.academy.trafficviolationsystem.user.UserEntity;
import com.academy.trafficviolationsystem.user.UserRepository;
import com.academy.trafficviolationsystem.violation.ViolationEntity;
import com.academy.trafficviolationsystem.violation.ViolationRepository;
import com.academy.trafficviolationsystem.violation.ViolationService;
import com.academy.trafficviolationsystem.violation.ViolationStatus;
import jakarta.persistence.EntityManager;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Business logic for the appeal domain.
 *
 * Implements BaseCRUDService — insert(), update(), search(), findById()
 * handled by the base. Domain-specific operations below.
 *
 * Status transitions (startReview/approve/reject/withdraw, and the
 * SUBMITTED-only edit check in beforeUpdate) are delegated to the
 * appeal.state package — see AppealState. This service no longer branches
 * on AppealStatus directly; it asks the entity's current state whether a
 * transition is legal and, if so, what status comes next. All side effects
 * (persistence, cross-module calls, audit) stay here.
 *
 * Key operations:
 *   beforeInsert   — validate window, no duplicate active appeal, load relationships
 *   afterInsert    — mark violation DISPUTED, mark fine DISPUTED
 *   beforeUpdate   — only editable in SUBMITTED status
 *   approve()      — officer approves: cancel fine, dismiss violation, reverse points
 *   reject()       — officer rejects: reinstate fine and violation
 *   withdraw()     — citizen withdraws: reinstate fine and violation
 *   startReview()  — officer picks up: SUBMITTED → UNDER_REVIEW
 *
 * Cross-module write directions (all one-way, no circular deps):
 *   appeal/ → violation/ : markDisputed, reinstateAfterAppealRejection, updateStatus
 *   appeal/ → fine/      : markDisputed, cancel, reinstateAfterAppealRejection
 *   appeal/ → driver/    : removePenaltyPoints
 */
@Service
@Transactional
public class AppealService implements BaseCRUDService<
        ViolationAppealEntity, AppealDto, AppealSearchObject,
        AppealCreateRequest, AppealUpdateRequest, UUID> {

    // Appeal window in days — replace with SystemConfigService.getInt("APPEAL_WINDOW_DAYS") later
    private static final int APPEAL_WINDOW_DAYS = 30;

    private final AppealRepository    appealRepository;
    private final ViolationRepository violationRepository;
    private final ViolationService    violationService;
    private final FineRepository      fineRepository;
    private final FineService         fineService;
    private final DriverRepository    driverRepository;
    private final DriverService       driverService;
    private final UserRepository      userRepository;
    private final AppealMapper        appealMapper;
    private final EntityManager       entityManager;

    public AppealService(AppealRepository appealRepository,
                         ViolationRepository violationRepository,
                         ViolationService violationService,
                         FineRepository fineRepository,
                         FineService fineService,
                         DriverRepository driverRepository,
                         DriverService driverService,
                         UserRepository userRepository,
                         AppealMapper appealMapper,
                         EntityManager entityManager) {
        this.appealRepository    = appealRepository;
        this.violationRepository = violationRepository;
        this.violationService    = violationService;
        this.fineRepository      = fineRepository;
        this.fineService         = fineService;
        this.driverRepository    = driverRepository;
        this.driverService       = driverService;
        this.userRepository      = userRepository;
        this.appealMapper        = appealMapper;
        this.entityManager       = entityManager;
    }

    // ── BaseCRUDService wiring ────────────────────────────────────────────

    @Override public AppealRepository              getRepository()    { return appealRepository; }
    @Override public EntityManager                 getEntityManager() { return entityManager;    }
    @Override public AppealMapper                  getMapper()        { return appealMapper;     }
    @Override public Class<ViolationAppealEntity>  getEntityClass()   { return ViolationAppealEntity.class; }

    // ── lifecycle hooks ───────────────────────────────────────────────────

    /**
     * Validates the appeal and loads all relationships before the entity is saved.
     *
     * Checks:
     *  1. Violation exists and is in CONFIRMED or DISPUTED status.
     *  2. No other active appeal exists for this violation.
     *  3. Appeal is submitted within APPEAL_WINDOW_DAYS of the violation.
     *  4. Resolves the driver from the violation (appeals are always filed for
     *     the violation's assigned driver — officers file on behalf of drivers).
     *  5. Resolves fineId from FineRepository if a fine exists.
     */

    @Override
    @AuditAction(value = "CREATE_APPEAL", entityClass = ViolationAppealEntity.class)
    public AppealDto insert(AppealCreateRequest request) {
        return BaseCRUDService.super.insert(request);
    }

    @Override
    @AuditAction(value = "UPDATE_APPEAL", entityClass = ViolationAppealEntity.class)
    public AppealDto update(UUID id, AppealUpdateRequest request) {
        return BaseCRUDService.super.update(id, request);
    }

    @Override
    public void beforeInsert(AppealCreateRequest request, ViolationAppealEntity entity) {

        // 1. Load and validate violation
        ViolationEntity violation = violationRepository.findById(request.getViolationId())
                .orElseThrow(() -> new NotFoundException(
                        "Violation " + request.getViolationId() + " not found"));

        if (violation.getStatus() != ViolationStatus.CONFIRMED
                && violation.getStatus() != ViolationStatus.DISPUTED) {
            throw new ViolationNotAppealableException(violation.getStatus());
        }

        if (violation.getDriver() == null) {
            throw new BadRequestException(
                    "Cannot file an appeal for violation " + violation.getReferenceNumber() +
                            " — no driver is assigned to it.");
        }

        // 2. Check for existing active appeal
        appealRepository.findActiveByViolationId(request.getViolationId())
                .ifPresent(existing -> {
                    throw new AppealAlreadyExistsException(existing.getAppealNumber(), violation.getReferenceNumber());
                });

        // 3. Appeal window check
        long daysSinceViolation = ChronoUnit.DAYS.between(
                violation.getOccurredAt().toLocalDate(), LocalDate.now());

        if (daysSinceViolation > APPEAL_WINDOW_DAYS) {
            throw new AppealWindowClosedException(
                    violation.getReferenceNumber(), daysSinceViolation - APPEAL_WINDOW_DAYS, APPEAL_WINDOW_DAYS);
        }

        // 4. Set relationships
        entity.setViolation(violation);
        entity.setDriver(violation.getDriver());

        // 5. Link fine if one exists for this violation
        fineRepository.findByViolationId(violation.getId())
                .ifPresent(fine -> entity.setFineId(fine.getId()));

        // 6. Set appeal metadata
        entity.setAppealNumber(generateAppealNumber());
        entity.setStatus(AppealStatus.SUBMITTED);
        entity.setSubmittedAt(LocalDateTime.now());
    }

    /**
     * After the appeal row is saved, mark the violation and fine as DISPUTED.
     * This blocks fine payment until the appeal is resolved.
     */
    @Override
    public void afterInsert(AppealCreateRequest request, ViolationAppealEntity entity) {
        violationService.markDisputed(entity.getViolation().getId());

        if (entity.getFineId() != null) {
            fineService.markDisputed(entity.getFineId());
        }
    }

    /**
     * Appeals can only be updated (reason/evidence) while still in SUBMITTED status.
     */
    @Override
    public void beforeUpdate(AppealUpdateRequest request, ViolationAppealEntity entity) {
        if (!currentState(entity).isEditable()) {
            throw new InvalidAppealStatusException(
                    "Appeal " + entity.getAppealNumber() +
                            " can no longer be edited — it is currently " + entity.getStatus());
        }
    }

    // ── search filters ────────────────────────────────────────────────────

    @Override
    public List<Predicate> additionalFilter(CriteriaBuilder cb,
                                            AppealSearchObject searchObj,
                                            Root<ViolationAppealEntity> root) {
        List<Predicate> predicates = new ArrayList<>();

        if (searchObj.getStatus() != null) {
            predicates.add(cb.equal(root.get("status"), searchObj.getStatus()));
        }
        if (searchObj.getDriverId() != null) {
            predicates.add(cb.equal(root.get("driver").get("id"), searchObj.getDriverId()));
        }
        if (searchObj.getViolationId() != null) {
            predicates.add(cb.equal(root.get("violation").get("id"), searchObj.getViolationId()));
        }
        if (searchObj.getReviewedById() != null) {
            predicates.add(cb.equal(root.get("reviewedBy").get("id"), searchObj.getReviewedById()));
        }
        if (searchObj.getFromDate() != null) {
            predicates.add(cb.greaterThanOrEqualTo(
                    root.get("submittedAt"), searchObj.getFromDate().atStartOfDay()));
        }
        if (searchObj.getToDate() != null) {
            predicates.add(cb.lessThan(
                    root.get("submittedAt"), searchObj.getToDate().plusDays(1).atStartOfDay()));
        }
        return predicates;
    }

    // ── review operations ─────────────────────────────────────────────────

    /**
     * Officer assigns themselves to review the appeal.
     * Transition: SUBMITTED → UNDER_REVIEW
     */
    @Transactional
    @AuditAction(value = "START_APPEAL_REVIEW", entityClass = ViolationAppealEntity.class)
    public AppealDto startReview(UUID appealId, UserPrincipal principal) {
        ViolationAppealEntity appeal = findEntityById(appealId);

        AppealState next = currentState(appeal).startReview(appeal.getAppealNumber());

        appeal.setStatus(next.getStatus());
        appeal.setReviewedBy(loadUser(principal.getId()));
        appealRepository.save(appeal);
        return toDtoWithDetails(appeal);
    }

    /**
     * Officer approves the appeal.
     * Transition: SUBMITTED or UNDER_REVIEW → APPROVED
     *
     * Side effects:
     *   1. Fine is cancelled — penalty points reversed by FineService.cancel().
     *   2. Violation is set to DISMISSED.
     */
    @Transactional
    @AuditAction(value = "APPROVE_APPEAL", entityClass = ViolationAppealEntity.class)
    public AppealDto approve(UUID appealId, ReviewAppealRequest request, UserPrincipal principal) {
        ViolationAppealEntity appeal = findEntityById(appealId);
        AppealState next = currentState(appeal).approve(appeal.getAppealNumber());

        UserEntity reviewer = loadUser(principal.getId());
        appeal.setStatus(next.getStatus());
        appeal.setReviewedBy(reviewer);
        appeal.setReviewedAt(LocalDateTime.now());
        appeal.setReviewNotes(request.getReviewNotes());
        appealRepository.save(appeal);

        // Cancel the fine — FineService.cancel() also reverses penalty points via DriverService
        if (appeal.getFineId() != null) {
            fineService.cancel(appeal.getFineId(),
                    "Appeal approved: " + appeal.getAppealNumber() + " — " + request.getReviewNotes(),
                    principal);
        }

        // Dismiss the violation
        dismissViolation(appeal.getViolation().getId(), reviewer, request.getReviewNotes());

        return toDtoWithDetails(appeal);
    }

    /**
     * Officer rejects the appeal.
     * Transition: SUBMITTED or UNDER_REVIEW → REJECTED
     *
     * Side effects:
     *   1. Fine reinstated to UNPAID.
     *   2. Violation reinstated to CONFIRMED.
     */
    @Transactional
    @AuditAction(value = "REJECT_APPEAL", entityClass = ViolationAppealEntity.class)
    public AppealDto reject(UUID appealId, ReviewAppealRequest request, UserPrincipal principal) {
        ViolationAppealEntity appeal = findEntityById(appealId);
        AppealState next = currentState(appeal).reject(appeal.getAppealNumber());

        appeal.setStatus(next.getStatus());
        appeal.setReviewedBy(loadUser(principal.getId()));
        appeal.setReviewedAt(LocalDateTime.now());
        appeal.setReviewNotes(request.getReviewNotes());
        appealRepository.save(appeal);

        // Reinstate fine and violation
        if (appeal.getFineId() != null) {
            fineService.reinstateAfterAppealRejection(appeal.getFineId());
        }
        violationService.reinstateAfterAppealRejection(appeal.getViolation().getId());

        return toDtoWithDetails(appeal);
    }

    /**
     * Driver withdraws the appeal before a decision.
     * Transition: SUBMITTED → WITHDRAWN
     * Only allowed in SUBMITTED status — cannot withdraw after review starts.
     *
     * @param appealId  The appeal to withdraw.
     * @param principal The authenticated user — must be the driver who filed the appeal.
     */
    @Transactional
    @AuditAction(value = "WITHDRAW_APPEAL", entityClass = ViolationAppealEntity.class)
    public AppealDto withdraw(UUID appealId, UserPrincipal principal) {
        ViolationAppealEntity appeal = findEntityById(appealId);
        AppealState next = currentState(appeal).withdraw(appeal.getAppealNumber());

        // Citizens can only withdraw their own appeals
        if (principal.isCitizen()) {
            DriverEntity driver = driverRepository.findByUserId(principal.getId()).orElse(null);
            if (driver == null || !driver.getId().equals(appeal.getDriver().getId())) {
                throw new AppealAccessDeniedException("You can only withdraw your own appeals");
            }
        }

        appeal.setStatus(next.getStatus());
        appeal.setReviewedAt(LocalDateTime.now());
        appeal.setReviewNotes("Withdrawn by driver");
        appealRepository.save(appeal);

        // Reinstate fine and violation — same as rejection
        if (appeal.getFineId() != null) {
            fineService.reinstateAfterAppealRejection(appeal.getFineId());
        }
        violationService.reinstateAfterAppealRejection(appeal.getViolation().getId());

        return toDtoWithDetails(appeal);
    }

    // ── read helpers ──────────────────────────────────────────────────────

    /**
     * Enriches the DTO with the violation's reference number from ViolationRepository.
     * MapStruct cannot resolve this inline — done here after mapping.
     */
    @Transactional(readOnly = true)
    public AppealDto toDtoWithDetails(ViolationAppealEntity appeal) {
        AppealDto dto = appealMapper.toDto(appeal);
        if (appeal.getViolation() != null) {
            dto.setViolationReference(appeal.getViolation().getReferenceNumber());
        }
        return dto;
    }

    /** All appeals for a specific driver — newest first. */
    @Transactional(readOnly = true)
    public List<AppealDto> getForDriver(UUID driverId) {
        return appealRepository.findByDriverIdOrderBySubmittedAtDesc(driverId)
                .stream()
                .map(this::toDtoWithDetails)
                .toList();
    }

    /** The officer review queue — all SUBMITTED appeals, oldest first. */
    @Transactional(readOnly = true)
    public List<AppealDto> getPendingReviewQueue() {
        return appealRepository.findByStatusOrderBySubmittedAtAsc(AppealStatus.SUBMITTED)
                .stream()
                .map(this::toDtoWithDetails)
                .toList();
    }

    // ── private helpers ───────────────────────────────────────────────────

    /** Resolves the entity's current status to its behaviour-carrying state. */
    private AppealState currentState(ViolationAppealEntity appeal) {
        return AppealStates.of(appeal.getStatus());
    }

    /**
     * Dismisses a violation as part of an appeal approval.
     * Appends the appeal decision to the violation's notes for audit trail.
     */
    private void dismissViolation(UUID violationId, UserEntity reviewer, String notes) {
        violationRepository.findById(violationId).ifPresent(violation -> {
            violation.setStatus(com.academy.trafficviolationsystem.violation.ViolationStatus.DISMISSED);
            violation.setReviewedBy(reviewer);
            violation.setReviewedAt(LocalDateTime.now());
            String entry = "[DISMISSED via appeal] " + notes;
            violation.setNotes(violation.getNotes() == null
                    ? entry : violation.getNotes() + "\n" + entry);
            violationRepository.save(violation);
        });
    }

    private UserEntity loadUser(UUID userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("User " + userId + " not found"));
    }

    /**
     * Generates the next appeal reference number.
     * Format: APP-{YEAR}-{6-digit-seq}, e.g. APP-2025-000001
     */
    private String generateAppealNumber() {
        int year = LocalDate.now().getYear();
        LocalDateTime yearStart = LocalDate.of(year, 1, 1).atStartOfDay();
        LocalDateTime yearEnd   = LocalDate.of(year + 1, 1, 1).atStartOfDay();
        long count = appealRepository.countByYear(yearStart, yearEnd);
        return String.format("APP-%d-%06d", year, count + 1);
    }
}