package com.academy.trafficviolationsystem.violation.workflow;

import com.academy.trafficviolationsystem.violation.ViolationConfirmedEvent;

/**
 * Coordinates the side effects that follow a confirmed violation:
 * fine issuance, penalty points, suspension notification, and the
 * fine-issued notification.
 *
 * This is the single place that knows about all four modules
 * (violation, fine, driver, notification) for this workflow — none
 * of those services call each other directly anymore for this flow.
 */
public interface ViolationWorkflowMediator {
    void onViolationConfirmed(ViolationConfirmedEvent event);
}