package com.academy.trafficviolationsystem.driver;

import com.academy.trafficviolationsystem.core.model.BaseSearchObject;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

/**
 * Search/filter parameters for GET /api/drivers.
 *
 * All fields are optional. Null fields are ignored by DriverService.additionalFilter().
 *
 * Example requests:
 *   GET /api/drivers?isSuspended=true
 *   GET /api/drivers?search=john&page=0&limit=20
 *   GET /api/drivers?licenseCategory=B&licenseExpired=false
 */
@Getter
@Setter
public class DriverSearchObject extends BaseSearchObject<UUID> {

    /**
     * Free-text search across firstName, lastName, licenseNumber, nationalId, email.
     * Case-insensitive LIKE with OR logic.
     */
    private String search;

    /** Filter by suspension status. */
    private Boolean isSuspended;

    /**
     * When true, only returns drivers whose licenseExpiresAt < today.
     * When false, only returns non-expired drivers.
     * Null returns all.
     */
    private Boolean licenseExpired;

    /** Filter by license category (exact match, e.g. "B"). */
    private String licenseCategory;
}
