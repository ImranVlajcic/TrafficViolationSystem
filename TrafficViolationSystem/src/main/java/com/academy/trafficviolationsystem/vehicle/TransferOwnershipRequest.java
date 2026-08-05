package com.academy.trafficviolationsystem.vehicle;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.util.UUID;

/**
 * Request body for POST /api/vehicles/{id}/transfer-ownership.
 *
 * Changing a vehicle's owner is a significant legal event — it needs its own
 * endpoint (not just a PUT field) so VehicleService can:
 *   1. Write a VehicleOwnershipHistoryEntity row.
 *   2. Validate the new owner exists and is active.
 *   3. Update VehicleEntity.owner atomically in the same transaction.
 */
@Getter
@Setter
public class TransferOwnershipRequest {

    @NotNull(message = "New owner ID is required")
    private UUID newOwnerId;

    /** Transfer date defaults to today in VehicleService if null. */
    private LocalDate transferDate;

    private String notes;
}
