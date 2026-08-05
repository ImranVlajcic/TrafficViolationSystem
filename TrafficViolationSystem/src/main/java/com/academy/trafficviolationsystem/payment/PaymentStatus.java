package com.academy.trafficviolationsystem.payment;

/**
 * Lifecycle state of a single PaymentEntity transaction.
 *
 * State machine:
 *   PENDING  → SUCCESS   (gateway confirms payment)
 *   PENDING  → FAILED    (gateway rejects)
 *   SUCCESS  → REFUNDED  (fine cancelled after payment — admin action)
 *   SUCCESS  → REVERSED  (chargeback or admin correction)
 *
 * FAILED is not terminal — a driver can retry with a different card.
 * Each retry creates a new PaymentEntity row (idempotency via transactionId).
 * SUCCESS, REFUNDED and REVERSED are terminal.
 */
public enum PaymentStatus {

    /** Transaction initiated, gateway response not yet received. */
    PENDING,

    /** Gateway confirmed — fine is now PAID. Terminal for the fine. */
    SUCCESS,

    /** Gateway rejected (insufficient funds, wrong card, daily limit exceeded). */
    FAILED,

    /** Money returned after the fine was cancelled post-payment. */
    REFUNDED,

    /** Chargeback or admin-initiated payment reversal. */
    REVERSED
}
