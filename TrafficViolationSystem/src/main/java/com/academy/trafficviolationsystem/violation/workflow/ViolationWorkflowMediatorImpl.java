package com.academy.trafficviolationsystem.violation.workflow;

import com.academy.trafficviolationsystem.driver.DriverEntity;
import com.academy.trafficviolationsystem.driver.DriverService;
import com.academy.trafficviolationsystem.fine.FineEntity;
import com.academy.trafficviolationsystem.fine.FinePdfService;
import com.academy.trafficviolationsystem.fine.FineService;
import com.academy.trafficviolationsystem.notification.NotificationService;
import com.academy.trafficviolationsystem.violation.ViolationConfirmedEvent;
import com.academy.trafficviolationsystem.violation.ViolationEntity;
import com.academy.trafficviolationsystem.violation.ViolationService;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.Optional;
import java.util.UUID;

/**
 * Sole listener for ViolationConfirmedEvent. Previously this logic lived
 * inside FineService.onViolationConfirmed(), which meant FineService had
 * to know about DriverService and reach into ViolationRepository directly
 * to back-link the fine. That coupling now lives here instead, and none
 * of the four domain services reference each other for this workflow.
 *
 * Sequence:
 *   1. FineService issues the fine (pure — no driver/violation writes)
 *   2. ViolationService.linkFine() back-links it (via its own public API,
 *      not a raw repository call from another module)
 *   3. DriverService applies penalty points, reporting back whether a
 *      suspension was triggered
 *   4. NotificationService sends the fine-issued notice, and the
 *      suspension notice if one was created
 *   5. FinePdfService generates the PDF async — deferred until after this
 *      transaction commits (see note on that call below)
 */
@Service
public class ViolationWorkflowMediatorImpl implements ViolationWorkflowMediator {

    private final FineService          fineService;
    private final ViolationService     violationService;
    private final DriverService        driverService;
    private final NotificationService  notificationService;
    private final FinePdfService       finePdfService;

    public ViolationWorkflowMediatorImpl(FineService fineService,
                                         ViolationService violationService,
                                         DriverService driverService,
                                         NotificationService notificationService,
                                         FinePdfService finePdfService) {
        this.fineService         = fineService;
        this.violationService    = violationService;
        this.driverService       = driverService;
        this.notificationService = notificationService;
        this.finePdfService      = finePdfService;
    }

    @Override
    @EventListener
    @Transactional
    public void onViolationConfirmed(ViolationConfirmedEvent event) {
        ViolationEntity violation = event.getViolation();

        Optional<FineEntity> maybeFine = fineService.issueFineForViolation(violation);
        if (maybeFine.isEmpty()) {
            return; // a fine already exists for this violation — nothing to orchestrate
        }
        FineEntity fine = maybeFine.get();
        DriverEntity driver = fine.getDriver();

        violationService.linkFine(violation.getId(), fine.getId());

        DriverService.PenaltyPointsResult penaltyResult = driverService.applyPenaltyPoints(
                driver.getId(),
                fine.getPenaltyPoints(),
                "VIOLATION " + violation.getReferenceNumber(),
                violation.getId());

        notificationService.sendFineIssuedNotification(fine);

        if (penaltyResult.suspensionCreated()) {
            notificationService.sendSuspensionNotification(driver, penaltyResult.suspension());
        }

        // generateFinePdf() runs on the pdfExecutor thread via @Async, and
        // re-fetches the fine by id in its own transaction (see
        // FinePdfService javadoc — this avoids a LazyInitializationException
        // on fine.getDriver() when reading the entity from a different
        // thread's session). Since this whole method is @Transactional,
        // firing that async call right here — before the transaction
        // commits — risks the PDF thread racing the commit and finding
        // nothing, or finding a stale pre-linkFine/pre-penalty-points view.
        // Deferring to afterCommit() avoids both. Falls back to firing
        // immediately if there's no active transaction synchronization
        // (e.g. a test invoking this listener directly, outside a tx).
        UUID fineId = fine.getId();
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    finePdfService.generateFinePdf(fineId);
                }
            });
        } else {
            finePdfService.generateFinePdf(fineId);
        }
    }
}
