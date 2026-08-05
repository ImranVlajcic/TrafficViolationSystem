package com.academy.trafficviolationsystem.violation;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

/**
 * Request body for POST /api/fine-rules.
 * Admin-only — used to create the initial rule for a ViolationType.
 * Only one active rule is allowed per type (enforced in FineRuleService.beforeInsert()).
 */
@Getter
@Setter
public class FineRuleCreateRequest {

    @NotNull(message = "Violation type is required")
    private ViolationType violationType;

    @NotNull(message = "Base amount is required")
    @DecimalMin(value = "0.01", message = "Base amount must be greater than zero")
    @Digits(integer = 8, fraction = 2)
    private BigDecimal baseAmount;

    @DecimalMin(value = "0.01")
    @Digits(integer = 8, fraction = 2)
    private BigDecimal minAmount;

    @DecimalMin(value = "0.01")
    @Digits(integer = 8, fraction = 2)
    private BigDecimal maxAmount;

    @NotNull(message = "Penalty points are required")
    @Min(value = 0)
    private Integer penaltyPoints;

    @Min(value = 1, message = "Payment due days must be at least 1")
    private Integer paymentDueDays = 30;

    @DecimalMin(value = "0.00")
    @Digits(integer = 3, fraction = 2)
    private BigDecimal earlyPayDiscountPct = BigDecimal.ZERO;

    @Min(value = 1)
    private Integer earlyPayWindowDays = 7;

    @DecimalMin(value = "0.00")
    @Digits(integer = 3, fraction = 2)
    private BigDecimal lateSurchargePct = new BigDecimal("0.10");

    private String description;
}
