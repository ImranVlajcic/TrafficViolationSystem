package com.academy.trafficviolationsystem.appeal;

import com.academy.trafficviolationsystem.core.entities.UUIDBaseEntity;
import com.academy.trafficviolationsystem.driver.DriverEntity;
import com.academy.trafficviolationsystem.user.UserEntity;
import com.academy.trafficviolationsystem.violation.ViolationEntity;
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
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * A driver's formal contest of a confirmed traffic violation.
 *
 * Extends UUIDBaseEntity — inherits id (UUID), created, updated,
 * createdBy, updatedBy, deletedAt (soft-delete).
 *
 * One appeal per active violation:
 *   Only one non-terminal (non-REJECTED, non-WITHDRAWN, non-APPROVED) appeal
 *   may exist per violation at any time. AppealService.beforeInsert() enforces
 *   this via AppealRepository.findActiveByViolationId().
 *
 * Fine link:
 *   fineId is stored as a raw UUID column (not a JPA @ManyToOne) to avoid
 *   a circular module dependency. appeal/ depends on fine/ for FineService
 *   calls, but FineEntity must not depend back on ViolationAppealEntity.
 *   AppealService loads FineEntity via FineRepository when it needs it.
 *
 * Appeal window:
 *   Appeals must be submitted within APPEAL_WINDOW_DAYS (default 30) of the
 *   violation.occurredAt timestamp. AppealService.beforeInsert() enforces this.
 *
 * Side effects on status transitions:
 *   SUBMITTED   → ViolationService.markDisputed(), FineService.markDisputed()
 *   APPROVED    → FineService.cancel(), ViolationService.dismiss equivalent,
 *                 DriverService.removePenaltyPoints()
 *   REJECTED    → FineService.reinstateAfterAppealRejection(),
 *                 ViolationService.reinstateAfterAppealRejection()
 *   WITHDRAWN   → same as REJECTED (fine and violation reinstated)
 */
@Getter
@Setter
@Entity
@Table(
    name = "violation_appeals",
    uniqueConstraints = {
        @UniqueConstraint(name = "uk_appeal_number", columnNames = "appeal_number")
    },
    indexes = {
        @Index(name = "idx_appeal_driver",    columnList = "driver_id"),
        @Index(name = "idx_appeal_violation", columnList = "violation_id"),
        @Index(name = "idx_appeal_status",    columnList = "status"),
        @Index(name = "idx_appeal_submitted", columnList = "submitted_at DESC")
    }
)
public class ViolationAppealEntity extends UUIDBaseEntity {

    // ── reference ─────────────────────────────────────────────────────────

    /**
     * Human-readable appeal reference.
     * Format: APP-{YEAR}-{6-digit-seq}, e.g. APP-2025-000001.
     * Printed on all appeal-related notifications and correspondence.
     */
    @Column(name = "appeal_number", nullable = false, length = 30)
    private String appealNumber;

    // ── appeal content ────────────────────────────────────────────────────

    /**
     * Driver's written grounds for the appeal.
     * Minimum 20 characters enforced by @Size on AppealCreateRequest
     * so appeals cannot be filed with trivial placeholder text.
     */
    @Column(name = "reason", nullable = false, columnDefinition = "TEXT")
    private String reason;

    /**
     * URL to a supporting document or image uploaded by the driver.
     * e.g. a photo showing the road sign was obscured, a witness statement.
     * Null if no evidence was attached.
     */
    @Column(name = "evidence_url")
    private String evidenceUrl;

    // ── status & timeline ─────────────────────────────────────────────────

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private AppealStatus status = AppealStatus.SUBMITTED;

    @Column(name = "submitted_at", nullable = false)
    private LocalDateTime submittedAt;

    /** Set when an officer makes a final decision (APPROVED or REJECTED). */
    @Column(name = "reviewed_at")
    private LocalDateTime reviewedAt;

    /**
     * Officer's mandatory written reasoning for the decision.
     * Required on both approval and rejection so there is always an audit trail.
     */
    @Column(name = "review_notes", columnDefinition = "TEXT")
    private String reviewNotes;

    // ── cross-module raw UUID link ─────────────────────────────────────────

    /**
     * FK to FineEntity stored as a raw UUID to avoid a circular JPA dependency.
     * appeal/ imports fine/ for FineService calls — FineEntity must not
     * import ViolationAppealEntity back. AppealService loads it via FineRepository.
     * Null when the driver is appealing the violation itself before a fine is issued
     * (rare but possible in the PENDING→CONFIRMED transition window).
     */
    @Column(name = "fine_id")
    private UUID fineId;

    // ── JPA relationships ─────────────────────────────────────────────────

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
        name = "violation_id",
        nullable = false,
        foreignKey = @ForeignKey(name = "fk_appeal_violation")
    )
    private ViolationEntity violation;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
        name = "driver_id",
        nullable = false,
        foreignKey = @ForeignKey(name = "fk_appeal_driver")
    )
    private DriverEntity driver;

    /**
     * Officer who reviewed and made the final decision.
     * Null until UNDER_REVIEW or until a direct APPROVED/REJECTED transition.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
        name = "reviewed_by_id",
        foreignKey = @ForeignKey(name = "fk_appeal_reviewer")
    )
    private UserEntity reviewedBy;
}
