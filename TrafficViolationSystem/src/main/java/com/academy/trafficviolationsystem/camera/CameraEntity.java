package com.academy.trafficviolationsystem.camera;

import com.academy.trafficviolationsystem.core.entities.AutoIdBaseEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * A physical traffic camera or radar device registered in the system.
 *
 * Extends AutoIdBaseEntity — cameras are config/infrastructure data,
 * integer PK is fine and avoids UUID overhead in the MQTT topic routing.
 *
 * MQTT integration:
 *   Each camera publishes detection events to a unique MQTT topic stored
 *   in mqttTopic. MqttConfig subscribes to the wildcard "cameras/#" so all
 *   registered cameras are covered. MqttViolationListener receives every
 *   message and looks up the camera by mqttTopic to get the device context.
 *
 * Heartbeat:
 *   CameraHeartbeatJob (runs every 5 minutes) marks cameras as offline
 *   when lastHeartbeatAt has not been updated within the threshold window.
 *   Online cameras publish a heartbeat message to cameras/{serial}/heartbeat
 *   which MqttHeartbeatListener handles — it updates lastHeartbeatAt and
 *   sets isOnline = true.
 *
 * Location:
 *   Coordinates are used to populate ViolationEntity.locationLatitude/Longitude
 *   when the MQTT payload does not carry GPS data (fixed cameras with known positions).
 */
@Getter
@Setter
@Entity
@SQLRestriction("deleted IS NULL")
@SQLDelete(sql = "UPDATE cameras SET deleted = now() WHERE id = ?")
@Table(
    name = "cameras",
    uniqueConstraints = {
        @UniqueConstraint(name = "uk_camera_serial", columnNames = "serial_number"),
        @UniqueConstraint(name = "uk_camera_topic",  columnNames = "mqtt_topic")
    },
    indexes = {
        @Index(name = "idx_camera_active",   columnList = "is_active"),
        @Index(name = "idx_camera_online",   columnList = "is_online"),
        @Index(name = "idx_camera_location", columnList = "latitude, longitude")
    }
)
public class CameraEntity extends AutoIdBaseEntity {

    // ── identification ────────────────────────────────────────────────────

    @Column(name = "serial_number", nullable = false, length = 60)
    private String serialNumber;

    @Column(name = "name", nullable = false, length = 120)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(name = "camera_type", nullable = false, length = 30)
    private CameraType cameraType;

    // ── location ──────────────────────────────────────────────────────────

    @Column(name = "latitude", nullable = false)
    private Double latitude;

    @Column(name = "longitude", nullable = false)
    private Double longitude;

    /** Compass bearing the camera faces, 0–359°. Null for omnidirectional. */
    @Column(name = "direction_degrees")
    private Integer directionDegrees;

    @Column(name = "location_description")
    private String locationDescription;

    // ── speed configuration ───────────────────────────────────────────────

    /**
     * Posted speed limit at this camera's position in km/h.
     * Used as the default speedLimit value when building ViolationCreateRequest
     * from an MQTT event that does not include the limit in its payload.
     * Null for non-speed cameras (RED_LIGHT, ANPR).
     */
    @Column(name = "speed_limit_kmh")
    private Integer speedLimitKmh;

    // ── MQTT ──────────────────────────────────────────────────────────────

    /**
     * Full MQTT topic this camera publishes detection events to.
     * Convention: cameras/{serialNumber}/events
     * e.g.        cameras/ILZ-RADAR-001/events
     *
     * Must be unique across all cameras — MqttViolationListener uses this
     * to look up the CameraEntity from the incoming message header.
     */
    @Column(name = "mqtt_topic", nullable = false, length = 200)
    private String mqttTopic;

    // ── status ────────────────────────────────────────────────────────────

    @Column(name = "is_online", nullable = false)
    private boolean isOnline = false;

    @Column(name = "last_heartbeat_at")
    private LocalDateTime lastHeartbeatAt;

    @Column(name = "is_active", nullable = false)
    private boolean isActive = true;

    // ── hardware info ─────────────────────────────────────────────────────

    @Column(name = "install_date")
    private LocalDate installDate;

    @Column(name = "firmware_version", length = 40)
    private String firmwareVersion;

    @Column(name = "zone_id")
    private Integer zoneId;
}
