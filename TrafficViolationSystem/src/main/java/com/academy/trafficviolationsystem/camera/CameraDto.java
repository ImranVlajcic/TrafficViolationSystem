package com.academy.trafficviolationsystem.camera;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Read-only projection of CameraEntity.
 *
 * minutesSinceHeartbeat is computed in CameraMapper @AfterMapping
 * so the frontend can show a "last seen X minutes ago" indicator
 * without formatting logic on the client.
 */
@Getter
@Setter
public class CameraDto {

    private Integer id;
    private String serialNumber;
    private String name;
    private CameraType cameraType;

    // location
    private Double latitude;
    private Double longitude;
    private Integer directionDegrees;
    private String locationDescription;

    // speed config
    private Integer speedLimitKmh;

    // mqtt
    private String mqttTopic;

    // status
    private boolean isOnline;
    private LocalDateTime lastHeartbeatAt;

    /**
     * Computed by CameraMapper @AfterMapping.
     * Minutes elapsed since lastHeartbeatAt. Null if never received a heartbeat.
     */
    private Long minutesSinceHeartbeat;

    private boolean isActive;

    // hardware
    private LocalDate installDate;
    private String firmwareVersion;
}
