package com.academy.trafficviolationsystem.driver;

import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.util.UUID;

/**
 * Request body for POST /api/drivers/{id}/suspend.
 * Used by officers and admins to manually suspend a driver's license.
 *
 * For automatic suspensions (triggered by penalty point threshold),
 * DriverService.applyPenaltyPoints() calls suspend() internally —
 * no HTTP request needed.
 */
@Getter
@Setter
public class SuspendDriverRequest {

    @NotBlank(message = "Reason is required")
    private String reason;

    /**
     * Scheduled lift date. Null = indefinite (requires manual lift).
     * When provided must be in the future — validated in DriverService.
     */
    @FutureOrPresent
    private LocalDate endDate;

    /**
     * The violation that triggered the manual suspension, if applicable.
     * Null for court orders or admin-initiated suspensions.
     */
    private UUID violationId;
}
