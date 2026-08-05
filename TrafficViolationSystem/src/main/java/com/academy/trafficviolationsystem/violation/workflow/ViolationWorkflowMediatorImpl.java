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

import java.util.Optional;

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
 *   5. FinePdfService generates the PDF async
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

        finePdfService.generateFinePdf(fine);
    }
}