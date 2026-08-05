package com.academy.trafficviolationsystem.analytics;

import com.academy.trafficviolationsystem.core.entities.UUIDBaseEntity;
import com.academy.trafficviolationsystem.user.UserEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "generated_reports")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class GeneratedReportEntity extends UUIDBaseEntity {

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ReportType reportType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ReportFormat format;

    @Column(nullable = true)
    private LocalDate periodStart;

    @Column(nullable = true)
    private LocalDate periodEnd;

    @Column(nullable = true)
    private String filePath;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ReportStatus status;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "requested_by_id", nullable = false)
    private UserEntity requestedBy;

    @Column(nullable = true)
    private LocalDateTime completedAt;

    @Column(nullable = true)
    private String errorMessage;

    /** JSON blob of extra filter parameters (zone, officer ID, driver ID, etc.) */
    @Column(columnDefinition = "TEXT", nullable = true)
    private String parameters;
}
