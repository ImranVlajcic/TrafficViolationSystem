package com.academy.trafficviolationsystem.fine;

import com.academy.trafficviolationsystem.core.model.BaseSearchObject;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.util.UUID;

/**
 * Search/filter parameters for GET /api/fines.
 *
 * All fields are optional. Null fields are ignored in FineService.additionalFilter().
 *
 * Example requests:
 *   GET /api/fines?status=UNPAID&driverId=<uuid>
 *   GET /api/fines?status=OVERDUE&includeCount=true
 *   GET /api/fines?issuedFrom=2025-01-01&issuedTo=2025-12-31
 */
@Getter
@Setter
public class FineSearchObject extends BaseSearchObject<UUID> {

    private FineStatus status;

    private UUID driverId;

    private UUID violationId;

    private UUID issuedById;

    /** Issued-at date range — start (inclusive). */
    private LocalDate issuedFrom;

    /** Issued-at date range — end (inclusive). */
    private LocalDate issuedTo;

    /**
     * When true, returns only fines whose dueDate < today.
     * Shortcut for the overdue dashboard — equivalent to status=OVERDUE
     * but works even if the job hasn't run yet.
     */
    private Boolean overdueDatePassed;

    /**
     * Free-text search across fineNumber.
     */
    private String search;
}
