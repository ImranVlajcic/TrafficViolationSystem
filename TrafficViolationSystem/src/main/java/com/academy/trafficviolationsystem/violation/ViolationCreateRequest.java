package com.academy.trafficviolationsystem.violation;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Request body for POST /api/violations.
 *
 * Used by two callers:
 *   1. Officers posting manually via the portal — detectionMethod = MANUAL_OFFICER.
 *   2. CameraEventProcessorService creating automatic violations — detectionMethod =
 *      CAMERA_AUTO or RADAR_AUTO.
 *
 * For manual violations the status will be set to CONFIRMED in ViolationService.beforeInsert().
 * For automatic violations the status starts PENDING until an officer reviews it.
 *
 * driverId is optional at creation — cameras detect plates, not always faces.
 * An officer can assign the driver later via PUT /api/violations/{id} before confirming.
 */
@Getter
@Setter
public class ViolationCreateRequest {

    @NotNull(message = "Violation type is required")
    private ViolationType violationType;

    @NotNull(message = "Detection method is required")
    private DetectionMethod detectionMethod;

    @NotNull(message = "Occurrence timestamp is required")
    private LocalDateTime occurredAt;

    @NotNull(message = "Location latitude is required")
    @DecimalMin(value = "-90.0",  message = "Latitude must be between -90 and 90")
    @DecimalMax(value = "90.0",   message = "Latitude must be between -90 and 90")
    private Double locationLatitude;

    @NotNull(message = "Location longitude is required")
    @DecimalMin(value = "-180.0", message = "Longitude must be between -180 and 180")
    @DecimalMax(value = "180.0",  message = "Longitude must be between -180 and 180")
    private Double locationLongitude;

    private String locationDescription;

    @Min(value = 0, message = "Measured speed must be positive")
    private Integer measuredSpeed;

    @Min(value = 0, message = "Speed limit must be positive")
    private Integer speedLimit;

    private String evidenceImageUrl;
    private String evidenceVideoUrl;
    private String notes;

    @NotNull(message = "Vehicle ID is required")
    private UUID vehicleId;

    /** Null if driver could not be identified at detection time. */
    private UUID driverId;

    /**
     * Camera or radar that detected the event.
     * Null for MANUAL_OFFICER violations.
     */
    private Long cameraId;
}
