package com.academy.trafficviolationsystem.notification;

/**
 * Dispatch state of a single NotificationEntity row.
 *
 * State machine:
 *   PENDING  ──► SENT      (first dispatch succeeded)
 *   PENDING  ──► FAILED    (first dispatch failed, no retries left)
 *   PENDING  ──► RETRYING  (first dispatch failed, retry scheduled)
 *   RETRYING ──► SENT      (retry succeeded)
 *   RETRYING ──► FAILED    (all retries exhausted)
 *
 * SENT and FAILED are terminal.
 */
public enum NotificationStatus {

    /** Persisted but not yet dispatched. */
    PENDING,

    /** Successfully delivered to the email/SMS provider. Terminal. */
    SENT,

    /** All retry attempts exhausted — delivery failed permanently. Terminal. */
    FAILED,

    /**
     * At least one dispatch attempt failed; nextRetryAt is set for the next attempt.
     * NotificationRetryJob picks up RETRYING rows whose nextRetryAt <= now().
     */
    RETRYING
}
