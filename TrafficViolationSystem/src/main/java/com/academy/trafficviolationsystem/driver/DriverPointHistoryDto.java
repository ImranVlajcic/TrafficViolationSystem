package com.academy.trafficviolationsystem.driver;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Read-only projection of DriverPointHistoryEntity.
 * Returned by GET /api/drivers/{id}/points.
 */
@Getter
@Setter
public class DriverPointHistoryDto {

    private UUID id;
    private int changeAmount;
    private int pointsBefore;
    private int pointsAfter;
    private String reason;
    private UUID violationId;
    private LocalDateTime occurredAt;
}
