package com.academy.trafficviolationsystem.camera;

import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

/**
 * Request body for POST /api/cameras/{id}/maintenance.
 * Used to log a completed or scheduled maintenance visit.
 */
@Getter
@Setter
public class LogMaintenanceRequest {

    @NotNull(message = "Maintenance type is required")
    private MaintenanceType maintenanceType;

    /** If null, treated as unscheduled/immediate. */
    @FutureOrPresent
    private LocalDate scheduledDate;

    private String firmwareBefore;
    private String firmwareAfter;
    private String notes;

    /**
     * True if the maintenance is already completed (default).
     * False if this is a future scheduled entry.
     */
    private boolean isCompleted = true;
}
