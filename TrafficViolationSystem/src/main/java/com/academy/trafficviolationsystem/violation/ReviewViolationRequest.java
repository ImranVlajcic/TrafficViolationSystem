package com.academy.trafficviolationsystem.violation;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

/**
 * Request body for:
 *   POST /api/violations/{id}/confirm
 *   POST /api/violations/{id}/dismiss
 *
 * The action itself (confirm vs dismiss) is encoded in the URL path,
 * not in this body. The body carries only the officer's written notes
 * about the decision, which are required so there is always an audit trail.
 *
 * For confirm: reviewNotes should explain why the violation is valid
 * (e.g. "Speed verified by calibrated radar unit #ILZ-007").
 * For dismiss: reviewNotes must explain why it was dismissed
 * (e.g. "Vehicle plate misread by OCR — confirmed by manual check").
 */
@Getter
@Setter
public class ReviewViolationRequest {

    @NotNull(message = "Review notes are required")
    private String reviewNotes;
}
