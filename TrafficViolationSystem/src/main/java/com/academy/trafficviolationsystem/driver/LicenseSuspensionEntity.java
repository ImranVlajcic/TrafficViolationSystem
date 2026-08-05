package com.academy.trafficviolationsystem.driver;

import com.academy.trafficviolationsystem.core.entities.UUIDBaseEntity;
import com.academy.trafficviolationsystem.user.UserEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;

import java.time.LocalDate;
import java.util.UUID;

/**
 * Immutable audit record of a single suspension period for a driver.
 *
 * A new row is written every time a driver is suspended — it is never
 * updated or soft-deleted. This gives a full timeline of all suspension
 * history that officers and admins can query.
 *
 * Lifecycle:
 *   1. DriverService.suspend() creates the row with isActive = true.
 *   2. LicenseSuspensionJob / DriverService.liftSuspension() sets
 *      isActive = false and liftedAt = today when the suspension ends.
 *
 * violationId is nullable — a suspension can be triggered manually by
 * an admin (e.g. court order) without a specific violation.
 */
@Getter
@Setter
@Entity
@SQLRestriction("deleted IS NULL")
@SQLDelete(sql = "UPDATE license_suspensions SET deleted = now() WHERE id = ?")
@Table(
    name = "license_suspensions",
    indexes = {
        @Index(name = "idx_susp_driver",    columnList = "driver_id, is_active"),
        @Index(name = "idx_susp_end_date",  columnList = "end_date")
    }
)
public class LicenseSuspensionEntity extends UUIDBaseEntity {

    @Column(name = "reason", nullable = false, columnDefinition = "TEXT")
    private String reason;

    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    /** Scheduled end date. Null = indefinite (e.g. court-ordered). */
    @Column(name = "end_date")
    private LocalDate endDate;

    /** Set when the suspension is lifted, whether at endDate or early. */
    @Column(name = "lifted_at")
    private LocalDate liftedAt;

    /** Points that caused the threshold to be crossed, captured at suspension time. */
    @Column(name = "points_at_time", nullable = false)
    private int pointsAtTime;

    @Column(name = "is_active", nullable = false)
    private boolean isActive = true;

    /**
     * The violation that caused the threshold crossing, if applicable.
     * Stored as raw UUID to avoid a circular FK dependency at the JPA level
     * (ViolationEntity lives in a different module that depends on driver/).
     */
    @Column(name = "violation_id")
    private UUID violationId;

    // ── relationships ─────────────────────────────────────────────────────

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
        name = "driver_id",
        nullable = false,
        foreignKey = @ForeignKey(name = "fk_susp_driver")
    )
    private DriverEntity driver;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
        name = "suspended_by_id",
        foreignKey = @ForeignKey(name = "fk_susp_officer")
    )
    private UserEntity suspendedBy;
}
