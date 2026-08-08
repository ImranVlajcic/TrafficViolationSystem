package com.academy.trafficviolationsystem.camera;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Read-only projection of CameraMaintenanceLogEntity.
 */
@Getter
@Setter
public class CameraMaintenanceLogDto {

    private UUID id;
    private MaintenanceType maintenanceType;
    private LocalDate scheduledDate;
    private LocalDateTime completedAt;
    private String firmwareBefore;
    private String firmwareAfter;
    private String notes;
    private boolean isCompleted;
    private Long cameraId;
    private UUID performedById;
    private String performedByUsername;
}
