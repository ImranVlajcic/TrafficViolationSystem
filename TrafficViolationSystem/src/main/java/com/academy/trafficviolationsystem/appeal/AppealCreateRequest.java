package com.academy.trafficviolationsystem.appeal;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

/**
 * Request body for POST /api/appeals.
 *
 * Filed by a CITIZEN via the portal, or by an OFFICER recording
 * a verbal appeal from a driver at a service centre.
 *
 * Business rules validated in AppealService.beforeInsert():
 *   - violation must exist and be in CONFIRMED or DISPUTED status
 *   - no other active (non-terminal) appeal for the same violation
 *   - appeal submitted within APPEAL_WINDOW_DAYS of violation.occurredAt
 *   - if filed by a CITIZEN, they must be the registered driver on the violation
 */
@Getter
@Setter
public class AppealCreateRequest {

    @NotNull(message = "Violation ID is required")
    private UUID violationId;

    /**
     * Driver's written grounds for the appeal.
     * Minimum 20 characters so appeals cannot be filed with placeholder text.
     * Maximum 5000 characters to keep submissions focused.
     */
    @NotBlank(message = "Reason is required")
    @Size(min = 20,  message = "Reason must be at least 20 characters")
    @Size(max = 5000, message = "Reason must not exceed 5000 characters")
    private String reason;

    /**
     * URL to a supporting document or image.
     * The actual file upload is handled by a separate file-upload endpoint
     * (outside scope of this module) — this field stores the resulting URL.
     * Null if no supporting evidence is being submitted.
     */
    private String evidenceUrl;
}
