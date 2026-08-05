package com.academy.trafficviolationsystem.camera;

import com.academy.trafficviolationsystem.core.entities.UUIDBaseEntity;
import com.academy.trafficviolationsystem.user.UserEntity;
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
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Log entry for a maintenance visit or operation on a camera.
 *
 * Written by CameraService.logMaintenance() — one row per visit.
 * Never updated after creation (append-only audit trail).
 *
 * isCompleted = false means a scheduled future maintenance that has not
 * happened yet. Once the technician completes the work they call
 * POST /api/cameras/{id}/maintenance/{logId}/complete which sets
 * isCompleted = true and completedAt = now.
 */
@Getter
@Setter
@Entity
@Table(
    name = "camera_maintenance_log",
    indexes = {
        @Index(name = "idx_maint_camera",    columnList = "camera_id, completed_at DESC"),
        @Index(name = "idx_maint_scheduled", columnList = "scheduled_date")
    }
)
public class CameraMaintenanceLogEntity extends UUIDBaseEntity {

    @Enumerated(EnumType.STRING)
    @Column(name = "maintenance_type", nullable = false, length = 30)
    private MaintenanceType maintenanceType;

    /** Planned date for the work — may be null for unscheduled emergency repairs. */
    @Column(name = "scheduled_date")
    private LocalDate scheduledDate;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    @Column(name = "firmware_before", length = 40)
    private String firmwareBefore;

    @Column(name = "firmware_after", length = 40)
    private String firmwareAfter;

    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;

    @Column(name = "is_completed", nullable = false)
    private boolean isCompleted = false;

    // ── relationships ─────────────────────────────────────────────────────

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
        name = "camera_id",
        nullable = false,
        foreignKey = @ForeignKey(name = "fk_maint_camera")
    )
    private CameraEntity camera;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
        name = "performed_by_id",
        foreignKey = @ForeignKey(name = "fk_maint_technician")
    )
    private UserEntity performedBy;
}
