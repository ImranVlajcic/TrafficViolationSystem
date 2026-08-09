package com.academy.trafficviolationsystem.fine;

import com.academy.trafficviolationsystem.core.entities.UUIDBaseEntity;
import com.academy.trafficviolationsystem.driver.DriverEntity;
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
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * A monetary penalty issued against a confirmed violation.
 *
 * Extends UUIDBaseEntity — inherits id (UUID), created, updated,
 * createdBy, updatedBy, deletedAt (soft-delete).
 *
 * Created by FineService when it receives a ViolationConfirmedEvent.
 * Never created directly through a POST /api/fines endpoint.
 *
 * Amount calculation:
 *   amount      = baseAmount from FineRuleEntity (copied at issuance time)
 *   discount    = earlyPayDiscount if paid within earlyPayWindowDays
 *   surcharge   = lateSurchargePct × amount, added by OverdueFineCheckerJob
 *   totalDue    = amount - discountAmount + surchargeAmount
 *   All amounts are copied from FineRuleEntity at creation and stored here
 *   so rule changes do not retroactively affect existing fines.
 *
 * Cross-module links:
 *   violationId — raw UUID, not a JPA FK. fine/ depends on violation/,
 *                 so a @ManyToOne back to ViolationEntity would be fine,
 *                 but ViolationEntity already has fineId as a raw UUID.
 *                 We keep both as raw UUIDs for symmetry and to avoid
 *                 accidental lazy-load issues on the violation side.
 *   driverId    — JPA FK to DriverEntity (fine/ depends on driver/).
 *   issuedBy    — JPA FK to UserEntity (officer or null for SYSTEM).
 *
 * PDF generation:
 *   pdfPath is null until FinePdfService generates the document.
 *   ViolationWorkflowMediatorImpl.onViolationConfirmed() calls
 *   FinePdfService.generateFinePdf() (which runs on the @Async pdfExecutor
 *   pool) as the last step of the fine-issuance workflow, after linking,
 *   penalty points, and notifications.
 */
@Getter
@Setter
@Entity
@Table(
    name = "fines",
    uniqueConstraints = {
        @UniqueConstraint(name = "uk_fine_number",    columnNames = "fine_number"),
        @UniqueConstraint(name = "uk_fine_violation", columnNames = "violation_id")
    },
    indexes = {
        @Index(name = "idx_fine_driver",  columnList = "driver_id"),
        @Index(name = "idx_fine_status",  columnList = "status"),
        @Index(name = "idx_fine_due",     columnList = "due_date"),
        @Index(name = "idx_fine_issued",  columnList = "issued_at DESC")
    }
)
// Enforce soft delete at the DB level: physical DELETE issued by Hibernate is
// rewritten to a deleted_at stamp, and every SELECT for this entity is
// transparently filtered to exclude soft-deleted rows. Without these two
// annotations, @PreRemove-only soft delete only updates the in-memory
// instance and Hibernate still issues a physical DELETE (confirmed recurring
// gap across Core/User/Driver/Camera/Vehicle — fixed here for Fine).
@SQLDelete(sql = "UPDATE fines SET deleted = now() WHERE id = ?")
@SQLRestriction("deleted IS NULL")
public class FineEntity extends UUIDBaseEntity {

    // ── reference ─────────────────────────────────────────────────────────

    /**
     * Human-readable fine number.
     * Format: FIN-{YEAR}-{6-digit-seq}, e.g. FIN-2025-000042.
     * Printed on all fine documents and payment receipts.
     */
    @Column(name = "fine_number", nullable = false, length = 30)
    private String fineNumber;

    // ── amounts (copied from FineRuleEntity at issuance — immutable snapshots) ──

    @Column(name = "amount", nullable = false, precision = 10, scale = 2)
    private BigDecimal amount;

    @Column(name = "currency", nullable = false, length = 3)
    private String currency = "BAM";

    /**
     * Early-payment discount applied if paid within earlyPayWindowDays.
     * Zero until the payment is processed and the discount is applicable.
     */
    @Column(name = "discount_amount", nullable = false, precision = 10, scale = 2)
    private BigDecimal discountAmount = BigDecimal.ZERO;

    /**
     * Late-payment surcharge added by OverdueFineCheckerJob when status → OVERDUE.
     * Zero until the due date is passed.
     */
    @Column(name = "surcharge_amount", nullable = false, precision = 10, scale = 2)
    private BigDecimal surchargeAmount = BigDecimal.ZERO;

    /** amount - discountAmount + surchargeAmount. Recomputed by FineService. */
    @Column(name = "total_due", nullable = false, precision = 10, scale = 2)
    private BigDecimal totalDue;

    // ── rule snapshots (copied at issuance so rule changes don't affect existing fines) ──

    /** Penalty points to apply when this fine is issued. */
    @Column(name = "penalty_points", nullable = false)
    private int penaltyPoints;

    /** Days after issuance when payment becomes overdue. */
    @Column(name = "payment_due_days", nullable = false)
    private int paymentDueDays;

    /** Discount percentage available for early payment. */
    @Column(name = "early_pay_discount_pct", nullable = false, precision = 5, scale = 2)
    private BigDecimal earlyPayDiscountPct;

    /** Window in days within which early-payment discount applies. */
    @Column(name = "early_pay_window_days", nullable = false)
    private int earlyPayWindowDays;

    /** Surcharge percentage applied once overdue. */
    @Column(name = "late_surcharge_pct", nullable = false, precision = 5, scale = 2)
    private BigDecimal lateSurchargePct;

    // ── dates ─────────────────────────────────────────────────────────────

    @Column(name = "issued_at", nullable = false)
    private LocalDateTime issuedAt;

    @Column(name = "due_date", nullable = false)
    private LocalDate dueDate;

    @Column(name = "paid_at")
    private LocalDateTime paidAt;

    // ── status ────────────────────────────────────────────────────────────

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private FineStatus status = FineStatus.UNPAID;

    // ── PDF ───────────────────────────────────────────────────────────────

    /** Set by FinePdfService after async generation. Null until PDF is ready. */
    @Column(name = "pdf_path")
    private String pdfPath;

    // ── cross-module raw UUID links ───────────────────────────────────────

    /**
     * The violation this fine was issued for.
     * Raw UUID to avoid bidirectional JPA dependency with violation/.
     */
    @Column(name = "violation_id", nullable = false)
    private UUID violationId;

    // ── JPA relationships ─────────────────────────────────────────────────

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
        name = "driver_id",
        nullable = false,
        foreignKey = @ForeignKey(name = "fk_fine_driver")
    )
    private DriverEntity driver;

    /**
     * Officer or system user who issued the fine.
     * Null when issued automatically by the event listener (SYSTEM context).
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
        name = "issued_by_id",
        foreignKey = @ForeignKey(name = "fk_fine_issued_by")
    )
    private UserEntity issuedBy;
}
