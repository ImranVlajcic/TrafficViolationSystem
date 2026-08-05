package com.academy.trafficviolationsystem.camera;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Read-only projection of CameraEventEntity.
 * Returned by GET /api/cameras/{id}/events for admin/officer review.
 */
@Getter
@Setter
public class CameraEventDto {

    private UUID id;
    private String mqttTopic;
    private String payload;
    private LocalDateTime receivedAt;

    // extracted
    private String licensePlate;
    private Integer measuredSpeed;
    private Double eventLatitude;
    private Double eventLongitude;
    private String imageUrl;

    // processing
    private boolean processed;
    private String processingError;
    private int retryCount;

    // links
    private UUID violationId;
    private Integer cameraId;
    private String cameraName;
}
