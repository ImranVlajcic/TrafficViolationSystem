package com.academy.trafficviolationsystem.appeal;

/**
 * Lifecycle states of a ViolationAppealEntity.
 *
 * State machine:
 *
 *   SUBMITTED ──► UNDER_REVIEW  (officer picks it up via start-review)
 *   SUBMITTED ──► WITHDRAWN     (driver cancels before decision)
 *   UNDER_REVIEW ──► APPROVED   (officer approves — fine cancelled, points reversed)
 *   UNDER_REVIEW ──► REJECTED   (officer rejects — fine reinstated to UNPAID)
 *   SUBMITTED ──► APPROVED      (officer can approve directly without UNDER_REVIEW step)
 *   SUBMITTED ──► REJECTED      (officer can reject directly without UNDER_REVIEW step)
 *
 * APPROVED, REJECTED and WITHDRAWN are terminal — no further transitions.
 * Only one non-terminal appeal is allowed per violation at a time.
 */
public enum AppealStatus {

    /**
     * Appeal filed by the driver, awaiting officer assignment.
     * Fine is in DISPUTED status — payment suspended.
     */
    SUBMITTED,

    /**
     * An officer has picked up the appeal for review.
     * Fine remains DISPUTED.
     */
    UNDER_REVIEW,

    /**
     * Officer approved the appeal — violation dismissed, fine cancelled,
     * penalty points reversed. Terminal.
     */
    APPROVED,

    /**
     * Officer rejected the appeal — violation upheld, fine reinstated to UNPAID.
     * Terminal.
     */
    REJECTED,

    /**
     * Driver withdrew the appeal before a decision was made.
     * Fine reinstated to UNPAID. Terminal.
     */
    WITHDRAWN
}
