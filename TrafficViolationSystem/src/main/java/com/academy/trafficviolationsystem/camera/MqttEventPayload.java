package com.academy.trafficviolationsystem.camera;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

/**
 * Deserialisation model for JSON payloads published by cameras and radars.
 *
 * Every device type sends a superset of this structure — unknown fields are
 * ignored (@JsonIgnoreProperties). Fields not present in a given payload type
 * remain null (e.g. measuredSpeedKmh is null for pure ANPR events).
 *
 * Expected JSON shape from a SPEED_RADAR:
 * {
 *   "plate":             "A123BC",
 *   "measuredSpeedKmh":  87,
 *   "speedLimitKmh":     60,
 *   "imageUrl":          "https://storage/images/ev_001.jpg",
 *   "videoUrl":          null,
 *   "latitude":          43.8563,
 *   "longitude":         18.4131,
 *   "timestampEpochMs":  1718000000000
 * }
 *
 * Expected JSON shape from a RED_LIGHT / ANPR camera:
 * {
 *   "plate":             "B456DE",
 *   "imageUrl":          "https://storage/images/ev_002.jpg",
 *   "videoUrl":          "https://storage/videos/ev_002.mp4",
 *   "latitude":          43.8501,
 *   "longitude":         18.3702,
 *   "timestampEpochMs":  1718000100000
 * }
 *
 * Heartbeat payload (published to cameras/{serial}/heartbeat):
 * {
 *   "serial": "ILZ-RADAR-001",
 *   "firmwareVersion": "2.4.1",
 *   "timestampEpochMs": 1718000200000
 * }
 */
@Getter
@Setter
@JsonIgnoreProperties(ignoreUnknown = true)
public class MqttEventPayload {

    /** License plate string as read by OCR. Null if plate recognition failed. */
    @JsonProperty("plate")
    private String plate;

    /** Speed measured by radar in km/h. Null for non-radar events. */
    @JsonProperty("measuredSpeedKmh")
    private Integer measuredSpeedKmh;

    /**
     * Speed limit at the camera location as known to the device firmware.
     * Null if the device does not transmit this — CameraEntity.speedLimitKmh is used instead.
     */
    @JsonProperty("speedLimitKmh")
    private Integer speedLimitKmh;

    /** URL to the captured still image stored in object storage. */
    @JsonProperty("imageUrl")
    private String imageUrl;

    /** URL to the captured video clip. Null for radar-only events. */
    @JsonProperty("videoUrl")
    private String videoUrl;

    /** GPS latitude from on-device GPS. Null for fixed cameras that rely on their registered position. */
    @JsonProperty("latitude")
    private Double latitude;

    /** GPS longitude from on-device GPS. */
    @JsonProperty("longitude")
    private Double longitude;

    /** Unix epoch timestamp in milliseconds from the device clock. */
    @JsonProperty("timestampEpochMs")
    private Long timestampEpochMs;

    // ── heartbeat-specific fields ─────────────────────────────────────────

    @JsonProperty("serial")
    private String serial;

    @JsonProperty("firmwareVersion")
    private String firmwareVersion;
}
