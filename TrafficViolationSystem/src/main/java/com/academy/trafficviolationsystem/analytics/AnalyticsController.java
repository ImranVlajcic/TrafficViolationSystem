package com.academy.trafficviolationsystem.analytics;

import com.academy.trafficviolationsystem.core.annotations.CurrentUser;
import com.academy.trafficviolationsystem.core.model.ApiResponse;
import com.academy.trafficviolationsystem.core.security.UserPrincipal;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

/**
 * REST controller for heatmap and statistics data.
 * Mapped to /api/analytics.
 *
 * Report generation and retrieval has been moved to ReportController
 * (/api/reports) so this controller is focused purely on read-only
 * analytics queries.
 *
 * Endpoints:
 *   GET /api/analytics/heatmap         → heatmap data for a date range
 *   GET /api/analytics/statistics      → KPI snapshot for a period
 *   GET /api/analytics/top-danger-zones→ top 10 highest-severity hotspots
 */
@RestController
@RequestMapping("/api/analytics")
@RequiredArgsConstructor
@Tag(name = "Analytics", description = "Heatmap data and KPI statistics")
public class AnalyticsController {

    private final AnalyticsService analyticsService;

    // ── Heatmap ───────────────────────────────────────────────────────────────

    @GetMapping("/heatmap")
    @PreAuthorize("isAuthenticated()")
    @Operation(
            summary = "Get violation heatmap data for a date range",
            description = "Returns pre-computed hotspot clusters. Falls back to a live " +
                    "query from the raw location log if the nightly job hasn't run yet. " +
                    "Feed the results directly into Leaflet.js HeatLayer or Google Maps heatmap."
    )
    public ResponseEntity<ApiResponse<List<HeatmapDataDto>>> getHeatmap(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return ResponseEntity.ok(
                ApiResponse.ok(analyticsService.getHeatmapData(from, to)));
    }

    // ── Statistics ────────────────────────────────────────────────────────────

    @GetMapping("/statistics")
    @PreAuthorize("hasAnyRole('OFFICER', 'ADMIN')")
    @Operation(
            summary = "Get KPI statistics snapshot for a period",
            description = "Returns a pre-computed snapshot. ViolationAggregatorJob generates " +
                    "snapshots at 00:30 each night. Returns 404 if no snapshot exists yet " +
                    "for the requested period — try again after 00:30."
    )
    public ResponseEntity<ApiResponse<StatisticsDto>> getStatistics(
            @RequestParam PeriodType periodType,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return ResponseEntity.ok(
                ApiResponse.ok(analyticsService.getStatistics(periodType, from, to)));
    }

    // ── Top danger zones ──────────────────────────────────────────────────────

    @GetMapping("/top-danger-zones")
    @PreAuthorize("hasAnyRole('OFFICER', 'ADMIN')")
    @Operation(
            summary = "Get the 10 highest-severity accident hotspots",
            description = "Uses pre-computed severity scores from the nightly aggregation job. " +
                    "Defaults to yesterday's data if periodEnd is not provided."
    )
    public ResponseEntity<ApiResponse<List<HeatmapDataDto>>> getTopDangerZones(
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate periodEnd,
            @CurrentUser UserPrincipal principal) {
        LocalDate end = periodEnd != null ? periodEnd : LocalDate.now().minusDays(1);
        return ResponseEntity.ok(
                ApiResponse.ok(analyticsService.getTopDangerZones(end)));
    }
}
