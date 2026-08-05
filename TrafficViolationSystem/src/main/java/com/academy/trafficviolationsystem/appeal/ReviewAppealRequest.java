package com.academy.trafficviolationsystem.appeal;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

/**
 * Request body for:
 *   POST /api/appeals/{id}/approve
 *   POST /api/appeals/{id}/reject
 *
 * reviewNotes is mandatory on both actions so there is always a written
 * justification in the audit trail. This is a legal requirement — approvals
 * cancel fines and reverse penalty points, so the reasoning must be recorded.
 */
@Getter
@Setter
public class ReviewAppealRequest {

    @NotBlank(message = "Review notes are required")
    @Size(min = 10,   message = "Review notes must be at least 10 characters")
    @Size(max = 2000, message = "Review notes must not exceed 2000 characters")
    private String reviewNotes;
}
