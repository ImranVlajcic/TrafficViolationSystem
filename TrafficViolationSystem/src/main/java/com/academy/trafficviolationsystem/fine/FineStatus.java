package com.academy.trafficviolationsystem.fine;

/**
 * Lifecycle states of a FineEntity.
 *
 * State machine:
 *
 *   UNPAID ──► PAID      (payment confirmed)
 *   UNPAID ──► OVERDUE   (due date passed, set by OverdueFineCheckerJob)
 *   OVERDUE──► PAID      (payment still accepted after due date, surcharge applied)
 *   UNPAID ──► DISPUTED  (driver files an appeal, payment suspended)
 *   OVERDUE──► DISPUTED  (appeal still possible after due date)
 *   DISPUTED──► UNPAID   (appeal rejected, fine reinstated)
 *   DISPUTED──► CANCELLED(appeal approved, fine cancelled)
 *   UNPAID ──► CANCELLED (admin cancels, e.g. court order)
 *   OVERDUE──► CANCELLED (admin cancels)
 *
 * PAID and CANCELLED are terminal — no further transitions allowed.
 */
public enum FineStatus {

    /** Issued but not yet paid. Payment is due before dueDate. */
    UNPAID,

    /**
     * Due date has passed without payment.
     * Set automatically by OverdueFineCheckerJob at 01:00 each night.
     * A late surcharge (lateSurchargePct from FineRuleEntity) is applied
     * to totalDue by the job at the same time.
     */
    OVERDUE,

    /**
     * Driver has filed an appeal — payment is suspended.
     * Fine cannot be paid while DISPUTED; AppealService sets this.
     * Transitions to UNPAID (appeal rejected) or CANCELLED (appeal approved).
     */
    DISPUTED,

    /** Payment has been confirmed. Terminal state. */
    PAID,

    /**
     * Fine was cancelled — by appeal approval or admin action.
     * Terminal state. Penalty points are reversed by FineService.cancel().
     */
    CANCELLED
}
