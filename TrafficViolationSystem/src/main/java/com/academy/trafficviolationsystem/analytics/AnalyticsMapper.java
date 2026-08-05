package com.academy.trafficviolationsystem.analytics;

import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Component
public class AnalyticsMapper {

    public HeatmapDataDto toHeatmapDto(AccidentHotspotEntity e) {
        return HeatmapDataDto.builder()
                .latitude(e.getLatitude())
                .longitude(e.getLongitude())
                .violationCount(e.getViolationCount())
                .severityScore(e.getSeverityScore())
                .dominantType(e.getDominantType())
                .locationLabel(e.getLocationLabel())
                .build();
    }

    public StatisticsDto toStatisticsDto(SystemStatisticsEntity e) {
        double collectionRate = computeRate(e.getTotalCollected(), e.getTotalFinesAmount());
        double appealRate     = e.getAppealsSubmitted() == 0 ? 0.0
                : (double) e.getAppealsApproved() / e.getAppealsSubmitted() * 100;

        return StatisticsDto.builder()
                .periodType(e.getPeriodType())
                .periodStart(e.getPeriodStart())
                .periodEnd(e.getPeriodEnd())
                .totalViolations(e.getTotalViolations())
                .autoDetected(e.getAutoDetected())
                .manuallyRecorded(e.getManuallyRecorded())
                .totalFinesIssued(e.getTotalFinesIssued())
                .totalFinesAmount(e.getTotalFinesAmount())
                .totalCollected(e.getTotalCollected())
                .totalOverdue(e.getTotalOverdue())
                .appealsSubmitted(e.getAppealsSubmitted())
                .appealsApproved(e.getAppealsApproved())
                .activeCameras(e.getActiveCameras())
                .computedAt(e.getComputedAt())
                .collectionRate(round(collectionRate))
                .appealApprovalRate(round(appealRate))
                .build();
    }

    public ReportDto toReportDto(GeneratedReportEntity e) {
        return ReportDto.builder()
                .id(e.getId())
                .reportType(e.getReportType())
                .format(e.getFormat())
                .periodStart(e.getPeriodStart())
                .periodEnd(e.getPeriodEnd())
                .status(e.getStatus())
                .isReady(e.getStatus() == ReportStatus.DONE)
                // filePath excluded — not exposed to clients
                .requestedById(e.getRequestedBy().getId())
                .requestedByUsername(e.getRequestedBy().getUsername())
                .completedAt(e.getCompletedAt())
                .build();
    }

    // ── helpers ──────────────────────────────────────────────────────────────

    private double computeRate(BigDecimal numerator, BigDecimal denominator) {
        if (denominator == null || denominator.compareTo(BigDecimal.ZERO) == 0) return 0.0;
        return numerator.divide(denominator, 6, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100))
                .doubleValue();
    }

    private double round(double value) {
        return BigDecimal.valueOf(value).setScale(2, RoundingMode.HALF_UP).doubleValue();
    }
}