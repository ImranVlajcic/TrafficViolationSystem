package com.academy.trafficviolationsystem.appeal;

import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

/**
 * Request body for PUT /api/appeals/{id}.
 *
 * A driver can update their appeal only while it is still in SUBMITTED status
 * (before an officer picks it up for review). AppealService.beforeUpdate()
 * throws 409 CONFLICT if the appeal has already moved to UNDER_REVIEW or beyond.
 *
 * All fields are optional — null keeps the existing value.
 * Only reason and evidenceUrl are editable after creation.
 */
@Getter
@Setter
public class AppealUpdateRequest {

    @Size(min = 20,   message = "Reason must be at least 20 characters")
    @Size(max = 5000, message = "Reason must not exceed 5000 characters")
    private String reason;

    private String evidenceUrl;
}
