package com.academy.trafficviolationsystem.appeal;

import com.academy.trafficviolationsystem.core.annotations.CurrentUser;
import com.academy.trafficviolationsystem.core.controllers.BaseCRUDController;
import com.academy.trafficviolationsystem.core.model.ApiResponse;
import com.academy.trafficviolationsystem.core.security.UserPrincipal;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * REST controller for violation appeal management.
 * Mapped to /api/appeals.
 *
 * Inherits from BaseCRUDController (free):
 *   GET  /api/appeals          → search(AppealSearchObject) → PagedResult<AppealDto>
 *   GET  /api/appeals/{id}     → findById(UUID)             → AppealDto
 *   POST /api/appeals          → file a new appeal (CITIZEN or OFFICER on their behalf)
 *   PUT  /api/appeals/{id}     → update reason/evidence (SUBMITTED status only)
 *
 * Extra endpoints:
 *   POST /api/appeals/{id}/start-review    → OFFICER/ADMIN picks up the appeal
 *   POST /api/appeals/{id}/approve         → OFFICER/ADMIN approves
 *   POST /api/appeals/{id}/reject          → OFFICER/ADMIN rejects
 *   POST /api/appeals/{id}/withdraw        → CITIZEN withdraws their own appeal
 *   GET  /api/appeals/pending              → officer review queue (SUBMITTED appeals)
 *   GET  /api/appeals/driver/{driverId}    → all appeals for a specific driver
 */
@RestController
@RequestMapping("/api/appeals")
@Tag(name = "Appeals", description = "Violation appeal filing and review workflow")
public class AppealController implements BaseCRUDController<
        ViolationAppealEntity, AppealDto, AppealSearchObject,
        AppealCreateRequest, AppealUpdateRequest, UUID> {

    private final AppealService appealService;

    public AppealController(AppealService appealService) {
        this.appealService = appealService;
    }

    @Override
    public AppealService getService() {
        return appealService;
    }

    // ── override findById to return enriched DTO ──────────────────────────

    @Override
    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public AppealDto findById(@PathVariable UUID id) {

        UserPrincipal principal = (UserPrincipal) SecurityContextHolder
                .getContext()
                .getAuthentication()
                .getPrincipal();

        ViolationAppealEntity appeal = appealService.findEntityById(id);

        appealService.verifyCitizenOwnsDriver(principal, appeal.getDriver());

        return appealService.toDtoWithDetails(appeal);
    }

    // ── officer review actions ────────────────────────────────────────────

    @PostMapping("/{id}/start-review")
    @Operation(summary = "Assign yourself to review an appeal (OFFICER/ADMIN)",
               description = "Transitions the appeal from SUBMITTED to UNDER_REVIEW " +
                             "and records the reviewing officer.")
    @PreAuthorize("hasAnyRole('ADMIN', 'OFFICER')")
    public ResponseEntity<ApiResponse<AppealDto>> startReview(
            @PathVariable UUID id,
            @CurrentUser UserPrincipal principal) {
        AppealDto dto = appealService.startReview(id, principal);
        return ResponseEntity.ok(ApiResponse.ok("Appeal assigned for review", dto));
    }

    @PostMapping("/{id}/approve")
    @Operation(summary = "Approve an appeal — cancels the fine and reverses penalty points (OFFICER/ADMIN)",
               description = "Transitions SUBMITTED or UNDER_REVIEW → APPROVED. " +
                             "The associated fine is cancelled and penalty points are reversed. " +
                             "Review notes are mandatory.")
    @PreAuthorize("hasAnyRole('ADMIN', 'OFFICER')")
    public ResponseEntity<ApiResponse<AppealDto>> approve(
            @PathVariable UUID id,
            @Valid @RequestBody ReviewAppealRequest request,
            @CurrentUser UserPrincipal principal) {
        AppealDto dto = appealService.approve(id, request, principal);
        return ResponseEntity.ok(
                ApiResponse.ok("Appeal approved — fine cancelled and penalty points reversed", dto));
    }

    @PostMapping("/{id}/reject")
    @Operation(summary = "Reject an appeal — fine reinstated to UNPAID (OFFICER/ADMIN)",
               description = "Transitions SUBMITTED or UNDER_REVIEW → REJECTED. " +
                             "The associated fine is reinstated to UNPAID status. " +
                             "Review notes are mandatory.")
    @PreAuthorize("hasAnyRole('ADMIN', 'OFFICER')")
    public ResponseEntity<ApiResponse<AppealDto>> reject(
            @PathVariable UUID id,
            @Valid @RequestBody ReviewAppealRequest request,
            @CurrentUser UserPrincipal principal) {
        AppealDto dto = appealService.reject(id, request, principal);
        return ResponseEntity.ok(ApiResponse.ok("Appeal rejected — fine reinstated", dto));
    }

    // ── citizen withdraw ──────────────────────────────────────────────────

    @PostMapping("/{id}/withdraw")
    @Operation(summary = "Withdraw your own appeal (CITIZEN only)",
               description = "Only available while the appeal is in SUBMITTED status. " +
                             "The fine is reinstated to UNPAID on withdrawal.")
    @PreAuthorize("hasRole('CITIZEN')")
    public ResponseEntity<ApiResponse<AppealDto>> withdraw(
            @PathVariable UUID id,
            @CurrentUser UserPrincipal principal) {
        AppealDto dto = appealService.withdraw(id, principal);
        return ResponseEntity.ok(ApiResponse.ok("Appeal withdrawn — fine reinstated", dto));
    }

    // ── scoped list endpoints ─────────────────────────────────────────────

    @GetMapping("/pending")
    @Operation(summary = "Get all SUBMITTED appeals awaiting officer review (OFFICER/ADMIN)",
               description = "Returns appeals oldest-first so officers work through the queue in order.")
    @PreAuthorize("hasAnyRole('ADMIN', 'OFFICER')")
    public ResponseEntity<ApiResponse<List<AppealDto>>> getPendingQueue() {
        return ResponseEntity.ok(
                ApiResponse.ok(appealService.getPendingReviewQueue()));
    }

    @GetMapping("/driver/{driverId}")
    @Operation(summary = "Get all appeals for a specific driver")
    @PreAuthorize("hasAnyRole('ADMIN', 'OFFICER') or @securityHelper.isOwnDriverRecord(#driverId, principal)")
    public ResponseEntity<ApiResponse<List<AppealDto>>> getForDriver(
            @PathVariable UUID driverId,
            @CurrentUser UserPrincipal principal) {
        return ResponseEntity.ok(
                ApiResponse.ok(appealService.getForDriver(driverId)));
    }
}
