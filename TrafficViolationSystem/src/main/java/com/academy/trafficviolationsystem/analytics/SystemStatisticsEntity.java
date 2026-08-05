package com.academy.trafficviolationsystem.analytics;

import com.academy.trafficviolationsystem.core.entities.AutoIdBaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(
    name = "system_statistics",
    indexes = {
        @Index(name = "idx_stats_period", columnList = "period_type, period_start")
    }
)
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class SystemStatisticsEntity extends AutoIdBaseEntity {

    @Enumerated(EnumType.STRING)
    @Column(name = "period_type", nullable = false)
    private PeriodType periodType;

    @Column(nullable = false)
    private LocalDate periodStart;

    @Column(nullable = false)
    private LocalDate periodEnd;

    @Column(nullable = false)
    private int totalViolations = 0;

    @Column(nullable = false)
    private int autoDetected = 0;

    @Column(nullable = false)
    private int manuallyRecorded = 0;

    @Column(nullable = false)
    private int totalFinesIssued = 0;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal totalFinesAmount = BigDecimal.ZERO;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal totalCollected = BigDecimal.ZERO;

    @Column(nullable = false)
    private int totalOverdue = 0;

    @Column(nullable = false)
    private int appealsSubmitted = 0;

    @Column(nullable = false)
    private int appealsApproved = 0;

    @Column(nullable = false)
    private int activeCameras = 0;

    @Column(nullable = false)
    private LocalDateTime computedAt;
}
