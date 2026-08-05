package com.academy.trafficviolationsystem.analytics;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

/**
 * Request body for POST /api/reports.
 *
 * periodStart and periodEnd are optional — some report types (e.g. DRIVER_HISTORY)
 * span all time. For period-based reports (MONTHLY_FINES, WEEKLY_ACTIVITY) both
 * should be provided; ReportService validates this per reportType.
 *
 * parameters is an optional JSON string for extra filters:
 *   DRIVER_HISTORY  → {"driverId": "<uuid>"}
 *   OFFICER_ACTIVITY→ {"officerId": "<uuid>"}
 *   ZONE_RANKING    → {"zoneId": 5}
 *   CAMERA_UPTIME   → {} (no extra params needed)
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ReportRequestDto {

    @NotNull(message = "Report type is required")
    private ReportType reportType;

    @NotNull(message = "Report format is required")
    private ReportFormat format;

    private LocalDate periodStart;

    private LocalDate periodEnd;

    /**
     * Optional JSON blob of extra filter parameters.
     * Content depends on reportType — see class-level Javadoc.
     */
    private String parameters;
}
