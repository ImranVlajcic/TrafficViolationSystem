package com.academy.trafficviolationsystem.driver;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.util.UUID;

/**
 * Read-only projection of LicenseSuspensionEntity.
 * Returned by GET /api/drivers/{id}/suspensions.
 */
@Getter
@Setter
public class LicenseSuspensionDto {

    private UUID id;
    private String reason;
    private LocalDate startDate;
    private LocalDate endDate;
    private LocalDate liftedAt;
    private int pointsAtTime;
    private boolean isActive;
    private UUID violationId;
    private UUID suspendedById;
    private String suspendedByUsername;
}
