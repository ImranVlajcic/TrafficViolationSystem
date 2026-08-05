package com.academy.trafficviolationsystem.violation;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

/**
 * Read-only projection of FineRuleEntity.
 * Returned by GET /api/fine-rules and used internally by FineService
 * when calculating fine amounts at issuance time.
 */
@Getter
@Setter
public class FineRuleDto {

    private Integer id;
    private ViolationType violationType;
    private BigDecimal baseAmount;
    private BigDecimal minAmount;
    private BigDecimal maxAmount;
    private int penaltyPoints;
    private int paymentDueDays;
    private BigDecimal earlyPayDiscountPct;
    private int earlyPayWindowDays;
    private BigDecimal lateSurchargePct;
    private String description;
    private boolean isActive;
}
