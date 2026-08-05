package com.academy.trafficviolationsystem.violation;

/**
 * Lifecycle states of a ViolationEntity.
 *
 * State machine:
 *
 *   PENDING ──► CONFIRMED ──► CLOSED
 *      │             │
 *      └──► DISMISSED
 *
 *   CONFIRMED ──► DISPUTED  (driver files an appeal)
 *   DISPUTED  ──► CONFIRMED (appeal rejected)
 *   DISPUTED  ──► DISMISSED (appeal approved)
 *
 * Only CONFIRMED violations can have a FineEntity created against them.
 * DISMISSED violations cannot be re-opened.
 * CLOSED means the fine has been paid and the case is fully resolved.
 */
public enum ViolationStatus {

    /**
     * Just created — awaiting officer review (for automatic detections)
     * or auto-confirmed (for manual officer recordings).
     */
    PENDING,

    /**
     * Reviewed and confirmed as valid by an officer.
     * A FineEntity should be issued at this point.
     */
    CONFIRMED,

    /**
     * Driver has filed an appeal — fine payment is suspended until resolved.
     */
    DISPUTED,

    /**
     * Dismissed by an officer (false positive, insufficient evidence, etc.).
     * No fine is issued or retained. Penalty points not applied.
     */
    DISMISSED,

    /**
     * Fine has been paid and the case is fully closed.
     * Set by FineService when payment is confirmed.
     */
    CLOSED
}
