package com.academy.trafficviolationsystem.violation;

import com.academy.trafficviolationsystem.core.entities.AutoIdBaseEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;

import java.math.BigDecimal;

/**
 * Admin-editable configuration that defines fine amounts and penalty points
 * for each ViolationType.
 *
 * Extends AutoIdBaseEntity — config tables use integer PKs, not UUIDs.
 *
 * Why this exists:
 *   Fine amounts are set by law and change occasionally. Storing them in this
 *   table means an admin can update amounts through the API without touching
 *   code or redeploying. FineService looks up the rule for the violation type
 *   when creating a FineEntity and copies the amounts onto it.
 *
 * Caching:
 *   FineRuleService should be annotated @Cacheable so the DB isn't hit on
 *   every fine issuance. The cache must be evicted when an admin updates a rule.
 *
 * One row per ViolationType — enforced by the unique constraint.
 */
@Getter
@Setter
@Entity
@SQLRestriction("deleted IS NULL")
@SQLDelete(sql = "UPDATE fine_rules SET deleted = now() WHERE id = ?")
@Table(
    name = "fine_rules",
    uniqueConstraints = {
        @UniqueConstraint(name = "uk_fine_rule_type", columnNames = "violation_type")
    },
    indexes = {
        @Index(name = "idx_frule_active", columnList = "violation_type, is_active")
    }
)
public class FineRuleEntity extends AutoIdBaseEntity {

    @Enumerated(EnumType.STRING)
    @Column(name = "violation_type", nullable = false, length = 40)
    private ViolationType violationType;

    /** Standard fine amount in the configured currency (BAM by default). */
    @Column(name = "base_amount", nullable = false, precision = 10, scale = 2)
    private BigDecimal baseAmount;

    /**
     * Minimum fine — allows officer discretion for border cases.
     * Null if no minimum (use baseAmount always).
     */
    @Column(name = "min_amount", precision = 10, scale = 2)
    private BigDecimal minAmount;

    /**
     * Maximum fine — upper bound for officer discretion.
     * Null if no maximum (use baseAmount always).
     */
    @Column(name = "max_amount", precision = 10, scale = 2)
    private BigDecimal maxAmount;

    /** Demerit points applied to the driver's license when the fine is issued. */
    @Column(name = "penalty_points", nullable = false)
    private int penaltyPoints = 0;

    /** Number of days after issuance before the fine becomes OVERDUE. */
    @Column(name = "payment_due_days", nullable = false)
    private int paymentDueDays = 30;

    /**
     * Percentage discount on base_amount for paying within earlyPayWindowDays.
     * Stored as a decimal fraction, e.g. 0.10 = 10% discount.
     */
    @Column(name = "early_pay_discount_pct", nullable = false, precision = 5, scale = 2)
    private BigDecimal earlyPayDiscountPct = BigDecimal.ZERO;

    /** Days from issuance within which the early-payment discount applies. */
    @Column(name = "early_pay_window_days", nullable = false)
    private int earlyPayWindowDays = 7;

    /**
     * Surcharge rate applied after the due date.
     * Stored as a decimal fraction, e.g. 0.10 = 10% surcharge.
     */
    @Column(name = "late_surcharge_pct", nullable = false, precision = 5, scale = 2)
    private BigDecimal lateSurchargePct = new BigDecimal("0.10");

    /**
     * Legal description printed on the fine PDF.
     * Should reference the specific law article for the violation type.
     */
    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    /** Inactive rules are skipped — no new fines are issued using them. */
    @Column(name = "is_active", nullable = false)
    private boolean isActive = true;
}
