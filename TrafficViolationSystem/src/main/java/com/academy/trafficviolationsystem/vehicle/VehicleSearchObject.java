package com.academy.trafficviolationsystem.vehicle;

import com.academy.trafficviolationsystem.core.model.BaseSearchObject;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

/**
 * Search/filter parameters for GET /api/vehicles.
 *
 * All fields are optional. Null fields are ignored in VehicleService.additionalFilter().
 *
 * Example requests:
 *   GET /api/vehicles?licensePlate=A123BC
 *   GET /api/vehicles?ownerId=<uuid>&isActive=true
 *   GET /api/vehicles?vehicleType=TRUCK&isStolen=false
 *   GET /api/vehicles?search=toyota&registrationExpired=true
 */
@Getter
@Setter
public class VehicleSearchObject extends BaseSearchObject<UUID> {

    /**
     * Free-text search across licensePlate, make, model, vin.
     * Case-insensitive LIKE with OR logic.
     */
    private String search;

    /** Exact license plate match (case-insensitive). */
    private String licensePlate;

    /** Filter to vehicles owned by a specific driver. */
    private UUID ownerId;

    /** Filter by vehicle type. */
    private VehicleType vehicleType;

    /** Filter by fuel type. */
    private FuelType fuelType;

    /** null = all, true = only stolen, false = only non-stolen. */
    private Boolean isStolen;

    /** null = all, true = only active, false = only deregistered. */
    private Boolean isActive;

    /**
     * When true, only returns vehicles where registrationExpiry < today.
     * When false, only non-expired. Null returns all.
     */
    private Boolean registrationExpired;

    /** Filter by manufacture year (exact). */
    private Integer year;
}
