package com.academy.trafficviolationsystem.violation;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Min;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

/**
 * Request body for PUT /api/fine-rules/{id}.
 * All fields optional — null keeps existing value.
 * violationType is immutable after creation.
 */
@Getter
@Setter
public class FineRuleUpdateRequest {

    @DecimalMin(value = "0.01")
    @Digits(integer = 8, fraction = 2)
    private BigDecimal baseAmount;

    @DecimalMin(value = "0.01")
    @Digits(integer = 8, fraction = 2)
    private BigDecimal minAmount;

    @DecimalMin(value = "0.01")
    @Digits(integer = 8, fraction = 2)
    private BigDecimal maxAmount;

    @Min(value = 0)
    private Integer penaltyPoints;

    @Min(value = 1)
    private Integer paymentDueDays;

    @DecimalMin(value = "0.00")
    @Digits(integer = 3, fraction = 2)
    private BigDecimal earlyPayDiscountPct;

    @Min(value = 1)
    private Integer earlyPayWindowDays;

    @DecimalMin(value = "0.00")
    @Digits(integer = 3, fraction = 2)
    private BigDecimal lateSurchargePct;

    private String description;

    private Boolean isActive;
}
