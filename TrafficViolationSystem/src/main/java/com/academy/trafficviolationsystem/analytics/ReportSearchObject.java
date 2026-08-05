package com.academy.trafficviolationsystem.analytics;

import com.academy.trafficviolationsystem.core.model.BaseSearchObject;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.util.UUID;

/**
 * Search/filter parameters for GET /api/reports.
 *
 * All fields optional. Null fields are ignored in ReportService.additionalFilter().
 *
 * Example requests:
 *   GET /api/reports?status=DONE
 *   GET /api/reports?reportType=MONTHLY_FINES&format=PDF
 *   GET /api/reports?requestedById=<uuid>
 */
@Getter
@Setter
public class ReportSearchObject extends BaseSearchObject<UUID> {

    private ReportStatus status;

    private ReportType reportType;

    private ReportFormat format;

    /** Filter to reports requested by a specific user. */
    private UUID requestedById;

    /** Created-at date range — start (inclusive). */
    private LocalDate fromDate;

    /** Created-at date range — end (inclusive). */
    private LocalDate toDate;
}
