package com.academy.trafficviolationsystem.analytics;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Read-only projection of GeneratedReportEntity returned by all report endpoints.
 *
 * filePath is intentionally absent — server filesystem paths must never
 * be exposed in API responses. Use GET /api/reports/{id}/download to
 * stream the file once isReady = true.
 *
 * isReady is a computed flag (status == DONE) set by ReportMapper @AfterMapping.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReportDto {

    private UUID id;
    private ReportType reportType;
    private ReportFormat format;
    private LocalDate periodStart;
    private LocalDate periodEnd;
    private ReportStatus status;

    /**
     * Computed by ReportMapper @AfterMapping.
     * True when status == DONE — the file is ready to download.
     */
    private boolean isReady;

    // filePath excluded — never exposed to clients

    private UUID requestedById;
    private String requestedByUsername;
    private LocalDateTime completedAt;
    private String errorMessage;
}
