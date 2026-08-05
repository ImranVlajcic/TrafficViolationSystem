package com.academy.trafficviolationsystem.vehicle;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.util.UUID;

/**
 * Read-only projection of VehicleOwnershipHistoryEntity.
 * Returned by GET /api/vehicles/{id}/ownership-history.
 */
@Getter
@Setter
public class VehicleOwnershipHistoryDto {

    private UUID id;
    private LocalDate transferDate;
    private String notes;

    private UUID previousOwnerId;
    private String previousOwnerFullName;
    private String previousOwnerLicenseNumber;

    private UUID newOwnerId;
    private String newOwnerFullName;
    private String newOwnerLicenseNumber;
}
