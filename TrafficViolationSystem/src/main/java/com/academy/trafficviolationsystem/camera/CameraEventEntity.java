package com.academy.trafficviolationsystem.camera;

import com.academy.trafficviolationsystem.core.entities.UUIDBaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Raw MQTT payload received from a camera or radar device.
 *
 * This is an append-only buffer — rows are written by MqttViolationListener
 * the moment a message arrives and are never modified except for two fields:
 *   processed      — set to true once CameraEventProcessorService creates a ViolationEntity
 *   processingError — set if processing fails, so the row can be retried
 *
 * Why keep raw events?
 *   1. Audit trail — the original payload is preserved regardless of what
 *      the processor does with it.
 *   2. Retry — if ViolationService or VehicleService throws on processing,
 *      the raw row remains with processed=false so a retry job can pick it up.
 *   3. Debugging — compare the raw JSON to the resulting ViolationEntity.
 *
 * violationId is a raw UUID (not a JPA FK) to avoid a circular dependency —
 * camera/ depends on violation/ for ViolationService, but ViolationEntity
 * has cameraId as a raw UUID pointing back. Keeping both sides as raw UUIDs
 * keeps the dependency one-directional: camera/ → violation/, never the reverse.
 */
@Getter
@Setter
@Entity
@Table(
    name = "camera_events",
    indexes = {
        @Index(name = "idx_event_camera",    columnList = "camera_id"),
        @Index(name = "idx_event_processed", columnList = "processed"),
        @Index(name = "idx_event_received",  columnList = "received_at DESC"),
        @Index(name = "idx_event_plate",     columnList = "license_plate")
    }
)
public class CameraEventEntity extends UUIDBaseEntity {

    // ── MQTT metadata ─────────────────────────────────────────────────────

    @Column(name = "mqtt_topic", nullable = false, length = 200)
    private String mqttTopic;

    /** Full raw JSON payload exactly as received from the broker. */
    @Column(name = "payload", nullable = false, columnDefinition = "TEXT")
    private String payload;

    @Column(name = "received_at", nullable = false)
    private LocalDateTime receivedAt;

    // ── extracted fields (parsed from payload by MqttViolationListener) ──

    /** License plate extracted by OCR or radar. Null if OCR failed. */
    @Column(name = "license_plate", length = 20)
    private String licensePlate;

    /** Speed measured in km/h. Null for non-radar events. */
    @Column(name = "measured_speed")
    private Integer measuredSpeed;

    /** GPS latitude from the payload. Null if not present (use camera location). */
    @Column(name = "event_latitude")
    private Double eventLatitude;

    /** GPS longitude from the payload. Null if not present (use camera location). */
    @Column(name = "event_longitude")
    private Double eventLongitude;

    /** Image URL extracted from the payload. */
    @Column(name = "image_url")
    private String imageUrl;

    // ── processing status ─────────────────────────────────────────────────

    /**
     * Set to true once CameraEventProcessorService successfully creates
     * a ViolationEntity from this event. Unprocessed events (processed=false)
     * are periodically retried by a scheduled job.
     */
    @Column(name = "processed", nullable = false)
    private boolean processed = false;

    /**
     * Populated when processing fails — stores the exception message.
     * Cleared on successful retry.
     */
    @Column(name = "processing_error", columnDefinition = "TEXT")
    private String processingError;

    /** How many processing attempts have been made including the initial one. */
    @Column(name = "retry_count", nullable = false)
    private int retryCount = 0;

    // ── links ─────────────────────────────────────────────────────────────

    /**
     * ViolationEntity created from this event.
     * Raw UUID — set by CameraEventProcessorService after violation insert.
     */
    @Column(name = "violation_id")
    private UUID violationId;

    // ── relationship ──────────────────────────────────────────────────────

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
        name = "camera_id",
        nullable = false,
        foreignKey = @ForeignKey(name = "fk_event_camera")
    )
    private CameraEntity camera;
}
