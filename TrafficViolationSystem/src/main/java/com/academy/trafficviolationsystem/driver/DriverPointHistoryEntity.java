package com.academy.trafficviolationsystem.driver;

import com.academy.trafficviolationsystem.core.entities.UUIDBaseEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Write-once log of every penalty point change for a driver.
 *
 * Never update or delete rows — this is an append-only audit trail.
 * It lets citizens, officers, and admins see the full history:
 *   "You received +3 points for TRF-2025-000123 on 2025-06-01"
 *   "Annual reset: -12 points on 2026-01-01"
 *   "Appeal approved: -3 points on 2025-08-14"
 *
 * changeAmount is positive for additions (violations) and negative
 * for reductions (annual reset, appeal approval).
 *
 * violationId is a raw UUID (not a JPA FK) to avoid a circular
 * dependency — the violation module depends on driver, not vice versa.
 */
@Getter
@Setter
@Entity
@SQLRestriction("deleted IS NULL")
@SQLDelete(sql = "UPDATE driver_point_history SET deleted = now() WHERE id = ?")
@Table(
    name = "driver_point_history",
    indexes = {
        @Index(name = "idx_dph_driver", columnList = "driver_id, occurred_at DESC")
    }
)
public class DriverPointHistoryEntity extends UUIDBaseEntity {

    /** Points added (positive) or removed (negative) in this event. */
    @Column(name = "change_amount", nullable = false)
    private int changeAmount;

    @Column(name = "points_before", nullable = false)
    private int pointsBefore;

    @Column(name = "points_after", nullable = false)
    private int pointsAfter;

    /**
     * Human-readable reason for this change.
     * Examples: "VIOLATION TRF-2025-000123", "ANNUAL_RESET", "APPEAL_APPROVED APP-2025-001"
     */
    @Column(name = "reason", nullable = false)
    private String reason;

    /** Raw FK to violation — null for non-violation events (resets, appeals). */
    @Column(name = "violation_id")
    private UUID violationId;

    @Column(name = "occurred_at", nullable = false)
    private LocalDateTime occurredAt;

    // ── relationship ──────────────────────────────────────────────────────

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
        name = "driver_id",
        nullable = false,
        foreignKey = @ForeignKey(name = "fk_dph_driver")
    )
    private DriverEntity driver;
}
