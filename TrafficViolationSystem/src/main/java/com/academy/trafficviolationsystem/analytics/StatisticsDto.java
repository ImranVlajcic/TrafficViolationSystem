package com.academy.trafficviolationsystem.analytics;

import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class StatisticsDto {

    private PeriodType periodType;
    private LocalDate periodStart;
    private LocalDate periodEnd;

    private int totalViolations;
    private int autoDetected;
    private int manuallyRecorded;

    private int totalFinesIssued;
    private BigDecimal totalFinesAmount;
    private BigDecimal totalCollected;
    private int totalOverdue;

    private int appealsSubmitted;
    private int appealsApproved;

    private int activeCameras;
    private LocalDateTime computedAt;

    /** totalCollected / totalFinesAmount × 100 — computed field */
    private Double collectionRate;

    /** appealsApproved / appealsSubmitted × 100 — computed field */
    private Double appealApprovalRate;
}
