package com.academy.trafficviolationsystem.appeal;

import com.academy.trafficviolationsystem.core.model.BaseSearchObject;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.util.UUID;

/**
 * Search/filter parameters for GET /api/appeals.
 *
 * All fields are optional. Null fields are ignored in AppealService.additionalFilter().
 *
 * Example requests:
 *   GET /api/appeals?status=SUBMITTED — officer review queue
 *   GET /api/appeals?driverId=<uuid>  — all appeals for a driver
 *   GET /api/appeals?status=APPROVED&fromDate=2025-01-01
 */
@Getter
@Setter
public class AppealSearchObject extends BaseSearchObject<UUID> {

    private AppealStatus status;

    private UUID driverId;

    private UUID violationId;

    /** Officer who reviewed the appeal. */
    private UUID reviewedById;

    /** submittedAt date range — start (inclusive). */
    private LocalDate fromDate;

    /** submittedAt date range — end (inclusive). */
    private LocalDate toDate;
}
