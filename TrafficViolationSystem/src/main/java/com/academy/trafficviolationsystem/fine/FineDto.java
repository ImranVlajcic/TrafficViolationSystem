package com.academy.trafficviolationsystem.fine;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Read-only projection of FineEntity.
 *
 * isEarlyPayEligible and daysUntilDue are computed by FineMapper @AfterMapping
 * so the frontend can show discount and urgency indicators without extra logic.
 */
@Getter
@Setter
public class FineDto {

    private UUID id;
    private String fineNumber;

    // amounts
    private BigDecimal amount;
    private BigDecimal discountAmount;
    private BigDecimal surchargeAmount;
    private BigDecimal totalDue;
    private String currency;

    // rule snapshots
    private int penaltyPoints;
    private int paymentDueDays;
    private BigDecimal earlyPayDiscountPct;
    private int earlyPayWindowDays;
    private BigDecimal lateSurchargePct;

    // dates
    private LocalDateTime issuedAt;
    private LocalDate dueDate;
    private LocalDateTime paidAt;

    // status
    private FineStatus status;

    /**
     * Computed by FineMapper @AfterMapping.
     * True when status is UNPAID and today is within earlyPayWindowDays of issuedAt.
     * Lets the frontend show a "Pay now — save X%" badge.
     */
    private boolean earlyPayEligible;

    /**
     * Computed by FineMapper @AfterMapping.
     * Days remaining until dueDate. Negative if already overdue.
     */
    private long daysUntilDue;

    // document
    private boolean pdfReady;

    // links
    private UUID violationId;
    private String violationReference;

    // driver summary
    private UUID driverId;
    private String driverFullName;
    private String driverLicenseNumber;

    // issuer
    private UUID issuedById;
    private String issuedByFullName;

    // audit
    private Instant created;
}
