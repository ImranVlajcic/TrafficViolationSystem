package com.academy.trafficviolationsystem.violation;

import com.academy.trafficviolationsystem.core.entities.UUIDBaseEntity;
import com.academy.trafficviolationsystem.driver.DriverEntity;
import com.academy.trafficviolationsystem.user.UserEntity;
import com.academy.trafficviolationsystem.vehicle.VehicleEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * The central record of a single traffic infraction.
 *
 * Extends UUIDBaseEntity — inherits id (UUID), created, updated,
 * createdBy, updatedBy, deletedAt (soft-delete).
 *
 * Creation paths:
 *   Automatic — CameraEventProcessorService receives an MQTT payload,
 *     resolves the plate to a VehicleEntity, and calls ViolationService.insert().
 *     Status starts PENDING and requires officer review.
 *   Manual — An officer posts to POST /api/violations directly.
 *     Status starts CONFIRMED immediately (officer has already witnessed it).
 *
 * FineEntity link:
 *   A FineEntity is created by FineService after ViolationService.confirm() is called.
 *   The link is kept as a raw UUID (fineId) rather than a JPA FK to avoid a circular
 *   dependency — fine/ depends on violation/, so violation/ cannot depend on fine/.
 *
 * Camera link:
 *   cameraId is a raw UUID for the same reason — camera/ depends on violation/.
 *
 * Location:
 *   GPS coordinates are stored on the entity for fast heatmap queries.
 *   ViolationService.afterInsert() also writes a ViolationLocationLogEntity row
 *   in the analytics module (raw UUID reference, no JPA FK).
 */
@Getter
@Setter
@Entity
@Table(
    name = "violations",
    uniqueConstraints = {
        @UniqueConstraint(name = "uk_violation_ref", columnNames = "reference_number")
    },
    indexes = {
        @Index(name = "idx_viol_vehicle",   columnList = "vehicle_id"),
        @Index(name = "idx_viol_driver",    columnList = "driver_id"),
        @Index(name = "idx_viol_status",    columnList = "status"),
        @Index(name = "idx_viol_occurred",  columnList = "occurred_at DESC"),
        @Index(name = "idx_viol_location",  columnList = "location_latitude, location_longitude")
    }
)
public class ViolationEntity extends UUIDBaseEntity {

    // ── reference number ──────────────────────────────────────────────────

    /**
     * Human-readable reference number generated in ViolationService.beforeInsert().
     * Format: TRF-{YEAR}-{6-digit-sequence}, e.g. TRF-2025-000123.
     * Printed on fine documents and used in all officer communications.
     */
    @Column(name = "reference_number", nullable = false, length = 30)
    private String referenceNumber;

    // ── violation classification ───────────────────────────────────────────

    @Enumerated(EnumType.STRING)
    @Column(name = "violation_type", nullable = false, length = 40)
    private ViolationType violationType;

    @Enumerated(EnumType.STRING)
    @Column(name = "detection_method", nullable = false, length = 30)
    private DetectionMethod detectionMethod;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private ViolationStatus status;

    // ── occurrence details ────────────────────────────────────────────────

    @Column(name = "occurred_at", nullable = false)
    private LocalDateTime occurredAt;

    @Column(name = "location_latitude", nullable = false)
    private Double locationLatitude;

    @Column(name = "location_longitude", nullable = false)
    private Double locationLongitude;

    /** Human-readable location label, e.g. "Zmaja od Bosne bb, Sarajevo". */
    @Column(name = "location_description")
    private String locationDescription;

    // ── speed data (only for SPEEDING violations) ─────────────────────────

    /** Speed measured by radar/camera in km/h. Null for non-speed violations. */
    @Column(name = "measured_speed")
    private Integer measuredSpeed;

    /** Posted speed limit at the detection point in km/h. */
    @Column(name = "speed_limit")
    private Integer speedLimit;

    // ── evidence ──────────────────────────────────────────────────────────

    @Column(name = "evidence_image_url")
    private String evidenceImageUrl;

    @Column(name = "evidence_video_url")
    private String evidenceVideoUrl;

    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;

    // ── automatic flag ────────────────────────────────────────────────────

    /**
     * True when created by the MQTT pipeline; false when manually recorded by an officer.
     * Determines the initial status (PENDING vs CONFIRMED) and UI presentation.
     */
    @Column(name = "is_automatic", nullable = false)
    private boolean isAutomatic;

    // ── raw UUID links (avoid circular JPA dependencies) ──────────────────

    /**
     * FK to CameraEntity — stored as raw UUID because camera/ depends on violation/,
     * so we cannot have a @ManyToOne pointing back to camera/.
     * Null for MANUAL_OFFICER violations.
     */
    @Column(name = "camera_id")
    private java.util.UUID cameraId;

    /**
     * FK to FineEntity — set by FineService after confirmation.
     * Null until a fine is issued.
     */
    @Column(name = "fine_id")
    private java.util.UUID fineId;

    // ── JPA relationships ─────────────────────────────────────────────────

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
        name = "vehicle_id",
        nullable = false,
        foreignKey = @ForeignKey(name = "fk_viol_vehicle")
    )
    private VehicleEntity vehicle;

    /**
     * The identified driver at the time of the violation.
     * May be null for automatic detections if the driver could not be determined
     * (e.g. the camera caught the plate but not the face).
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
        name = "driver_id",
        foreignKey = @ForeignKey(name = "fk_viol_driver")
    )
    private DriverEntity driver;

    /**
     * Officer who manually recorded the violation.
     * Null for automatic detections.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
        name = "officer_id",
        foreignKey = @ForeignKey(name = "fk_viol_officer")
    )
    private UserEntity officer;

    /**
     * Officer who reviewed and confirmed or dismissed the violation.
     * Null until a review decision is made.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
        name = "reviewed_by_id",
        foreignKey = @ForeignKey(name = "fk_viol_reviewer")
    )
    private UserEntity reviewedBy;

    @Column(name = "reviewed_at")
    private LocalDateTime reviewedAt;
}
