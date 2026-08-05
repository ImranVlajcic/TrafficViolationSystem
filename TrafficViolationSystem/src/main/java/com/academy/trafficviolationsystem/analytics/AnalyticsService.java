package com.academy.trafficviolationsystem.analytics;

import com.academy.trafficviolationsystem.core.exceptions.NotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Service for heatmap and statistics data.
 *
 * Report generation has been moved to ReportService — this service now
 * only handles heatmap and statistics queries so responsibilities are clean.
 *
 * Changes from original:
 *  - EntityNotFoundException (JPA) replaced with NotFoundException (core)
 *  - Report methods delegate to ReportService instead of duplicating logic
 *  - Principal replaced with UserPrincipal throughout
 */
@Service
@RequiredArgsConstructor
public class AnalyticsService {

    private final AnalyticsRepository            analyticsRepository;
    private final ViolationLocationLogRepository locationLogRepository;
    private final SystemStatisticsRepository     statisticsRepository;
    private final AnalyticsMapper                mapper;

    // ── Heatmap ───────────────────────────────────────────────────────────────

    /**
     * Returns pre-built hotspot data for the given date range.
     * Falls back to a live query from the raw location log if the nightly
     * ViolationAggregatorJob hasn't run yet for this period.
     */
    @Transactional(readOnly = true)
    public List<HeatmapDataDto> getHeatmapData(LocalDate from, LocalDate to) {
        List<AccidentHotspotEntity> hotspots =
                analyticsRepository.findByPeriodStartAndPeriodEnd(from, to);

        if (!hotspots.isEmpty()) {
            return hotspots.stream()
                    .map(mapper::toHeatmapDto)
                    .collect(Collectors.toList());
        }

        // Live fallback — build from raw log (nightly job hasn't run for this period)
        return locationLogRepository
                .clusterByGridCell(from.atStartOfDay(), to.plusDays(1).atStartOfDay(), 0.001)
                .stream()
                .map(row -> HeatmapDataDto.builder()
                        .latitude((Double) row[0])
                        .longitude((Double) row[1])
                        .violationCount(((Number) row[2]).intValue())
                        .severityScore(((Number) row[2]).doubleValue())
                        .dominantType((String) row[3])
                        .build())
                .collect(Collectors.toList());
    }

    // ── Statistics ────────────────────────────────────────────────────────────

    /**
     * Returns the pre-computed statistics snapshot for the given period.
     * Throws NotFoundException (not JPA EntityNotFoundException) if no
     * snapshot exists — ViolationAggregatorJob runs at 00:30 nightly.
     */
    @Transactional(readOnly = true)
    public StatisticsDto getStatistics(PeriodType periodType, LocalDate from, LocalDate to) {
        return statisticsRepository
                .findByPeriodTypeAndPeriodStartAndPeriodEnd(periodType, from, to)
                .map(mapper::toStatisticsDto)
                .orElseThrow(() -> new NotFoundException(
                        "No statistics snapshot found for " + periodType +
                                " " + from + " – " + to +
                                ". ViolationAggregatorJob generates snapshots nightly — try again after 00:30."));
    }

    // ── Top danger zones (used by dashboard) ──────────────────────────────────

    /**
     * Returns the 10 highest-severity hotspots for the most recent period end date.
     * Used by the dashboard to show the danger zone ranking card.
     */
    @Transactional(readOnly = true)
    public List<HeatmapDataDto> getTopDangerZones(LocalDate periodEnd) {
        return analyticsRepository
                .findTop10BySeverityScoreDescAndPeriodEnd(periodEnd)
                .stream()
                .map(mapper::toHeatmapDto)
                .collect(Collectors.toList());
    }
}
