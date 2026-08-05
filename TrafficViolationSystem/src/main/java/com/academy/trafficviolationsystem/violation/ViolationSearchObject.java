package com.academy.trafficviolationsystem.violation;

import com.academy.trafficviolationsystem.core.model.BaseSearchObject;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.util.UUID;

/**
 * Search/filter parameters for GET /api/violations.
 *
 * All fields are optional. Null fields are ignored in ViolationService.additionalFilter().
 *
 * Example requests:
 *   GET /api/violations?status=PENDING&page=0&limit=20
 *   GET /api/violations?driverId=<uuid>&includeCount=true
 *   GET /api/violations?violationType=SPEEDING&fromDate=2025-01-01&toDate=2025-12-31
 *   GET /api/violations?isAutomatic=true&status=PENDING
 */
@Getter
@Setter
public class ViolationSearchObject extends BaseSearchObject<UUID> {

    /** Filter by current lifecycle status. */
    private ViolationStatus status;

    /** Filter by infraction type. */
    private ViolationType violationType;

    /** Filter by detection method. */
    private DetectionMethod detectionMethod;

    /** Filter to violations for a specific vehicle. */
    private UUID vehicleId;

    /** Filter to violations for a specific driver. */
    private UUID driverId;

    /** Filter to violations recorded by a specific officer. */
    private UUID officerId;

    /** Filter to violations detected by a specific camera (raw UUID). */
    private UUID cameraId;

    /** null = all, true = only automatic, false = only manual. */
    private Boolean isAutomatic;

    /** Date range filter on occurredAt — start of range (inclusive). */
    private LocalDate fromDate;

    /** Date range filter on occurredAt — end of range (inclusive). */
    private LocalDate toDate;

    /**
     * Free-text search on referenceNumber, locationDescription, notes.
     * Case-insensitive LIKE with OR logic.
     */
    private String search;
}
