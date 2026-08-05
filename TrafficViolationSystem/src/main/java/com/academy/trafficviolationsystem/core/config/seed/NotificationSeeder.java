package com.academy.trafficviolationsystem.core.config.seed;

import com.academy.trafficviolationsystem.appeal.AppealStatus;
import com.academy.trafficviolationsystem.appeal.ViolationAppealEntity;
import com.academy.trafficviolationsystem.driver.DriverEntity;
import com.academy.trafficviolationsystem.driver.LicenseSuspensionEntity;
import com.academy.trafficviolationsystem.fine.FineEntity;
import com.academy.trafficviolationsystem.fine.FineStatus;
import com.academy.trafficviolationsystem.notification.NotificationEntity;
import com.academy.trafficviolationsystem.notification.NotificationRepository;
import com.academy.trafficviolationsystem.notification.NotificationStatus;
import com.academy.trafficviolationsystem.notification.NotificationType;
import com.academy.trafficviolationsystem.payment.PaymentEntity;
import com.academy.trafficviolationsystem.payment.PaymentStatus;
import com.academy.trafficviolationsystem.user.UserEntity;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Seeds one notification per fine-issued, payment-success, appeal-decided,
 * and active-suspension event — mostly SENT, with a small SENT/FAILED/RETRYING
 * spread so the notification-status filters have something to show.
 *
 * IMPORTANT: takes the fines/payments/appeals/suspensions lists directly from
 * the seeders that just created them in this same run, rather than re-querying
 * via a repository. Re-fetching fresh entities here would return them with
 * uninitialized LAZY proxies (fine.getDriver(), appeal.getDriver(), etc.), and
 * since this all runs outside a transaction, accessing those proxies later
 * throws LazyInitializationException: no session. Using the already-attached
 * in-memory objects (real references set via setDriver()/setFine() etc. during
 * creation, not lazy proxies) sidesteps the problem entirely.
 */
@Component
public class NotificationSeeder {

    private final NotificationRepository notificationRepository;

    public NotificationSeeder(NotificationRepository notificationRepository) {
        this.notificationRepository = notificationRepository;
    }

    public void seed(List<FineEntity> fines, List<PaymentEntity> payments,
                     List<ViolationAppealEntity> appeals, List<LicenseSuspensionEntity> suspensions) {
        if (notificationRepository.count() > 0) return;

        for (FineEntity fine : fines) {
            if (fine.getStatus() == FineStatus.CANCELLED) continue;
            DriverEntity driver = fine.getDriver();
            save(NotificationType.EMAIL,
                    "Izdata je nova kazna " + fine.getFineNumber(),
                    "Poštovani " + driverName(driver) + ", izdata vam je kazna " + fine.getFineNumber()
                            + " u iznosu od " + fine.getAmount() + " " + fine.getCurrency() + ".",
                    recipient(driver), driver != null ? driver.getUser() : null,
                    fine.getId(), "FINE", fine.getIssuedAt());
        }

        for (PaymentEntity payment : payments) {
            if (payment.getStatus() != PaymentStatus.SUCCESS) continue;
            DriverEntity driver = payment.getFine().getDriver();
            save(NotificationType.EMAIL,
                    "Uplata primljena",
                    "Vaša uplata za kaznu " + payment.getFine().getFineNumber()
                            + " je uspješno evidentirana. Transakcija: " + payment.getTransactionId(),
                    recipient(driver), payment.getPaidBy(),
                    payment.getId(), "PAYMENT", payment.getPaidAt());
        }

        for (ViolationAppealEntity appeal : appeals) {
            if (appeal.getStatus() != AppealStatus.APPROVED && appeal.getStatus() != AppealStatus.REJECTED) continue;
            DriverEntity driver = appeal.getDriver();
            boolean approved = appeal.getStatus() == AppealStatus.APPROVED;
            save(NotificationType.EMAIL,
                    approved ? "Vaša žalba je usvojena" : "Vaša žalba je odbijena",
                    "Žalba " + appeal.getAppealNumber() + " je " + (approved ? "usvojena." : "odbijena.")
                            + " Napomena: " + appeal.getReviewNotes(),
                    recipient(driver), driver != null ? driver.getUser() : null,
                    appeal.getId(), "APPEAL", appeal.getReviewedAt());
        }

        for (LicenseSuspensionEntity suspension : suspensions) {
            if (!suspension.isActive()) continue;
            DriverEntity driver = suspension.getDriver();
            save(NotificationType.EMAIL,
                    "Obavijest o suspenziji vozačke dozvole",
                    "Vaša vozačka dozvola je suspendovana do " + suspension.getEndDate()
                            + " zbog " + suspension.getPointsAtTime() + " kaznenih poena.",
                    recipient(driver), driver != null ? driver.getUser() : null,
                    suspension.getId(), "SUSPENSION", suspension.getStartDate().atStartOfDay());
        }
    }

    private void save(NotificationType type, String subject, String body, String recipient,
                      UserEntity user, UUID relatedEntityId, String relatedEntityType, LocalDateTime when) {
        NotificationEntity notification = new NotificationEntity();
        notification.setType(type);
        notification.setSubject(subject);
        notification.setBody(body);
        notification.setRecipient(recipient != null ? recipient : "unknown@traffic-academy.com");

        double r = SeedRandom.RNG.nextDouble();
        if (r < 0.85) {
            notification.setStatus(NotificationStatus.SENT);
            notification.setSentAt(when);
        } else if (r < 0.95) {
            notification.setStatus(NotificationStatus.RETRYING);
            notification.setRetryCount(SeedRandom.intBetween(1, 3));
            notification.setNextRetryAt(LocalDateTime.now().plusMinutes(SeedRandom.intBetween(5, 60)));
            notification.setFailureReason("SMTP timeout");
        } else {
            notification.setStatus(NotificationStatus.FAILED);
            notification.setRetryCount(4);
            notification.setFailureReason("Recipient address rejected");
        }

        notification.setRelatedEntityId(relatedEntityId);
        notification.setRelatedEntityType(relatedEntityType);
        notification.setUser(user);
        notificationRepository.save(notification);
    }

    private String recipient(DriverEntity driver) {
        return driver != null ? driver.getEmail() : null;
    }

    private String driverName(DriverEntity driver) {
        return driver != null ? driver.getFirstName() + " " + driver.getLastName() : "vozač";
    }
}