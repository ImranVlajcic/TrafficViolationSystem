package com.academy.trafficviolationsystem.configuration;

/**
 * Compile-time constants for every system_config key.
 *
 * Usage:
 *   int threshold = systemConfigService.getInt(ConfigKeys.SUSPENSION_THRESHOLD_POINTS);
 *
 * Keeping keys here prevents typos and makes IDE navigation / refactoring trivial.
 */
public final class ConfigKeys {

    private ConfigKeys() {}

    // ── DRIVER ────────────────────────────────────────────────────────────────
    public static final String SUSPENSION_THRESHOLD_POINTS  = "SUSPENSION_THRESHOLD_POINTS";
    public static final String SUSPENSION_DURATION_DAYS     = "SUSPENSION_DURATION_DAYS";
    public static final String ANNUAL_POINT_RESET_ENABLED   = "ANNUAL_POINT_RESET_ENABLED";

    // ── FINE ──────────────────────────────────────────────────────────────────
    public static final String EARLY_PAY_WINDOW_DAYS        = "EARLY_PAY_WINDOW_DAYS";
    public static final String EARLY_PAY_DISCOUNT_PERCENT   = "EARLY_PAY_DISCOUNT_PERCENT";
    public static final String OVERDUE_SURCHARGE_PERCENT    = "OVERDUE_SURCHARGE_PERCENT";
    public static final String APPEAL_WINDOW_DAYS           = "APPEAL_WINDOW_DAYS";

    // ── NOTIFICATION ─────────────────────────────────────────────────────────
    public static final String NOTIFICATION_MAX_RETRIES     = "NOTIFICATION_MAX_RETRIES";
    public static final String NOTIFICATION_RETRY_1_MINUTES = "NOTIFICATION_RETRY_1_MINUTES";
    public static final String NOTIFICATION_RETRY_2_MINUTES = "NOTIFICATION_RETRY_2_MINUTES";
    public static final String NOTIFICATION_RETRY_3_MINUTES = "NOTIFICATION_RETRY_3_MINUTES";

    // ── PDF ───────────────────────────────────────────────────────────────────
    public static final String PDF_HEADER_LOGO_URL          = "PDF_HEADER_LOGO_URL";
    public static final String PDF_HEADER_AGENCY_NAME       = "PDF_HEADER_AGENCY_NAME";
    public static final String PDF_OUTPUT_DIR               = "PDF_OUTPUT_DIR";

    // ── MQTT / CAMERAS ────────────────────────────────────────────────────────
    public static final String CAMERA_OFFLINE_THRESHOLD_MINUTES = "CAMERA_OFFLINE_THRESHOLD_MINUTES";
    public static final String CAMERA_EVENT_MAX_RETRY       = "CAMERA_EVENT_MAX_RETRY";
    public static final String MQTT_BROKER_URL              = "MQTT_BROKER_URL";
}
