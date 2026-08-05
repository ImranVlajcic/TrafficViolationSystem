package com.academy.trafficviolationsystem.violation;

import org.springframework.context.ApplicationEvent;

/**
 * Published by ViolationService when a violation transitions to CONFIRMED status.
 *
 * This happens in two cases:
 *   1. A manual violation is created (status = CONFIRMED immediately on insert).
 *   2. An officer calls POST /api/violations/{id}/confirm on a PENDING automatic violation.
 *
 * FineService listens for this event with @EventListener and creates the FineEntity.
 * This design keeps violation/ independent of fine/ — ViolationService does not
 * import or call FineService directly, so there is no circular module dependency.
 *
 * The event is synchronous by default (same transaction). If you want fine creation
 * to survive even if the violation transaction rolls back, annotate the listener
 * with @TransactionalEventListener(phase = AFTER_COMMIT) in FineService.
 */
public class ViolationConfirmedEvent extends ApplicationEvent {

    private final ViolationEntity violation;

    public ViolationConfirmedEvent(Object source, ViolationEntity violation) {
        super(source);
        this.violation = violation;
    }

    public ViolationEntity getViolation() {
        return violation;
    }
}
