package com.academy.trafficviolationsystem.violation;

/**
 * How the violation was detected and entered into the system.
 *
 * Drives different processing flows in ViolationService:
 *   CAMERA_AUTO / RADAR_AUTO → status starts PENDING, requires officer review
 *   MANUAL_OFFICER           → status starts CONFIRMED immediately
 */
public enum DetectionMethod {

    /** Fixed ANPR or speed camera — triggered by CameraEventProcessorService via MQTT. */
    CAMERA_AUTO,

    /** Fixed or mobile speed radar — triggered by CameraEventProcessorService via MQTT. */
    RADAR_AUTO,

    /** Traffic officer recorded the violation manually via the officer portal. */
    MANUAL_OFFICER
}
