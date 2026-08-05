package com.academy.trafficviolationsystem.core.exceptions;

/**
 * Canonical error codes returned in every ErrorResponse body.
 * Frontend apps switch on these values to show localized messages
 * without parsing the human-readable 'message' string.
 */
public enum ErrorCode {

    // ── generic ──────────────────────────────────────────────────────────
    RESOURCE_NOT_FOUND,
    VALIDATION_FAILED,
    BAD_REQUEST,
    UNKNOWN_ERROR,

    // ── auth / security ───────────────────────────────────────────────────
    UNAUTHORIZED,               // 401 – no valid token present
    FORBIDDEN,                  // 403 – token valid but role insufficient
    TOKEN_EXPIRED,              // 401 – JWT past its expiry
    TOKEN_INVALID,              // 401 – JWT signature or format wrong
    ACCOUNT_LOCKED,             // 423 – too many failed login attempts
    ACCOUNT_DISABLED,           // 403 – user soft-disabled by admin
    DUPLICATE_RESOURCE,         // 409 – unique constraint would be violated

    // ── vehicle domain ─────────────────────────────────────────────────

    VEHICLE_ALREADY_STOLEN,
    VEHICLE_NOT_STOLEN,
    VEHICLE_DEREGISTERED,

    // ── violation domain ─────────────────────────────────────────────────
    VIOLATION_ALREADY_CONFIRMED,  // cannot confirm an already-confirmed violation
    VIOLATION_ALREADY_DISMISSED,  // cannot act on a dismissed violation
    VIOLATION_ALREADY_HAS_FINE,   // fine already exists for this violation
    VIOLATION_CLOSED,

    // ── fine domain ───────────────────────────────────────────────────────
    FINE_ALREADY_PAID,            // payment attempted on a paid fine
    FINE_OVERDUE,                 // fine has passed due date, surcharge applies
    FINE_CANCELLED,               // fine was cancelled, no payment accepted
    FINE_ALREADY_CANCELLED,
    FINE_NOT_DISPUTABLE,
    VIOLATION_MISSING_DRIVER,

    // ── driver / license ─────────────────────────────────────────────────
    LICENSE_SUSPENDED,            // driver's license is currently suspended
    LICENSE_EXPIRED,              // driver's license has expired
    DRIVER_NOT_FOUND,             // no driver found for the given plate / id
    DRIVER_ALREADY_SUSPENDED,
    DRIVER_NOT_SUSPENDED,
    DRIVER_ALREADY_LINKED,
    USER_ALREADY_LINKED,

    // ── config ───────────────────────────────────────────────────────────
    CONFIG_TYPE_MISMATCH,
    CONFIG_CREATION_NOT_ALLOWED,
    CONFIG_READ_ONLY,

    // ── camera ───────────────────────────────────────────────────────────
    CONFLICT,

    // ── payment ───────────────────────────────────────────────────────────
    PAYMENT_FAILED,               // simulated gateway rejection
    PAYMENT_ALREADY_PROCESSED,    // idempotency guard – transaction already exists

    // ── appeal ────────────────────────────────────────────────────────────
    APPEAL_ALREADY_EXISTS,        // only one active appeal per violation allowed
    APPEAL_WINDOW_CLOSED,
    INVALID_APPEAL_STATUS,
    VIOLATION_NOT_APPEALABLE,// appeal period has elapsed

    // ── infrastructure ────────────────────────────────────────────────────
    MQTT_CONNECTION_ERROR,        // cannot reach MQTT broker
    PDF_GENERATION_ERROR,         // iText / PDF generation failed
    NOTIFICATION_SEND_ERROR       // email / SMS dispatch failed
}
