package com.academy.trafficviolationsystem.violation;

import jakarta.validation.constraints.Min;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

/**
 * Request body for PUT /api/violations/{id}.
 *
 * All fields are optional — null means keep existing value.
 * Used by officers to enrich an automatic violation before confirming it:
 *   - assign a driverId that the camera could not determine
 *   - add or correct notes, evidence URLs, location description
 *   - correct speed data if the raw MQTT payload was inaccurate
 *
 * Status transitions (confirm / dismiss) are handled by dedicated endpoints,
 * not through this update request, so that the transition logic
 * (business rules, side-effects) is always applied consistently.
 *
 * violationType, detectionMethod, vehicleId, and occurredAt are immutable
 * after creation — they represent what actually happened.
 */
@Getter
@Setter
public class ViolationUpdateRequest {

    /**
     * Assign or correct the identified driver.
     * Can be set after initial creation when the driver is determined.
     */
    private UUID driverId;

    private String locationDescription;

    @Min(value = 0)
    private Integer measuredSpeed;

    @Min(value = 0)
    private Integer speedLimit;

    private String evidenceImageUrl;
    private String evidenceVideoUrl;
    private String notes;
}
