package com.academy.trafficviolationsystem.violation;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Read-only projection of ViolationEntity returned by all violation endpoints.
 *
 * speedExcess is a computed field set by ViolationMapper @AfterMapping —
 * it is the difference between measuredSpeed and speedLimit, null for
 * non-speeding violations.
 *
 * Driver and vehicle summaries are denormalised here so the frontend does
 * not need extra requests to render a violation card.
 */
@Getter
@Setter
public class ViolationDto {

    private UUID id;

    // reference
    private String referenceNumber;

    // classification
    private ViolationType violationType;
    private DetectionMethod detectionMethod;
    private ViolationStatus status;
    private boolean isAutomatic;

    // occurrence
    private LocalDateTime occurredAt;
    private Double locationLatitude;
    private Double locationLongitude;
    private String locationDescription;

    // speed data
    private Integer measuredSpeed;
    private Integer speedLimit;

    /**
     * Computed by ViolationMapper @AfterMapping.
     * measuredSpeed - speedLimit. Null for non-speeding violations.
     */
    private Integer speedExcess;

    // evidence
    private String evidenceImageUrl;
    private String evidenceVideoUrl;
    private String notes;

    // vehicle summary
    private UUID vehicleId;
    private String vehicleLicensePlate;
    private String vehicleMakeModel;

    // driver summary
    private UUID driverId;
    private String driverFullName;
    private String driverLicenseNumber;

    // officer info
    private UUID officerId;
    private String officerFullName;
    private String officerBadgeNumber;

    // review info
    private UUID reviewedById;
    private String reviewedByFullName;
    private LocalDateTime reviewedAt;

    // linked entities
    private Long cameraId;
    private UUID fineId;

    // audit
    private LocalDateTime created;
    private String createdBy;
}
