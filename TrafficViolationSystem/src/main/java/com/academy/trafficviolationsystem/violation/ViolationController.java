package com.academy.trafficviolationsystem.violation;

import com.academy.trafficviolationsystem.core.annotations.CurrentUser;
import com.academy.trafficviolationsystem.core.controllers.BaseCRUDController;
import com.academy.trafficviolationsystem.core.model.ApiResponse;
import com.academy.trafficviolationsystem.core.security.SecurityHelper;
import com.academy.trafficviolationsystem.core.security.UserPrincipal;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

/**
 * REST controller for violation management.
 * Mapped to /api/violations.
 *
 * Inherits from BaseCRUDController (free):
 *   GET  /api/violations           → search(ViolationSearchObject) → PagedResult<ViolationDto>
 *   GET  /api/violations/{id}      → findById(UUID)                → ViolationDto
 *   POST /api/violations           → create(ViolationCreateRequest)→ ViolationDto
 *   PUT  /api/violations/{id}      → update(UUID, ViolationUpdateRequest) → ViolationDto
 *
 * Extra endpoints:
 *   POST /api/violations/{id}/confirm            → OFFICER/ADMIN
 *   POST /api/violations/{id}/dismiss            → OFFICER/ADMIN
 *   GET  /api/violations/driver/{driverId}       → all violations for a driver
 *   GET  /api/violations/vehicle/{vehicleId}     → all violations for a vehicle
 *   GET  /api/violations/pending                 → officer review queue
 *   GET  /api/violations/my                      → current citizen's own violations
 */
@RestController
@RequestMapping("/api/violations")
@Tag(name = "Violations", description = "Traffic violation recording and review")
public class ViolationController implements BaseCRUDController<
        ViolationEntity, ViolationDto, ViolationSearchObject, ViolationCreateRequest, ViolationUpdateRequest, UUID> {

    private final ViolationService violationService;
    private final SecurityHelper securityHelper;

    public ViolationController(ViolationService violationService, SecurityHelper securityHelper) {
        this.violationService = violationService;
        this.securityHelper = securityHelper;
    }

    @Override
    public ViolationService getService() {
        return violationService;
    }

    // ── review actions ────────────────────────────────────────────────────

    @PostMapping("/{id}/confirm")
    @Operation(summary = "Confirm a pending automatic violation as valid (OFFICER/ADMIN)",
            description = "Transitions PENDING → CONFIRMED and triggers fine issuance.")
    @PreAuthorize("hasAnyRole('ADMIN', 'OFFICER')")
    public ResponseEntity<ApiResponse<ViolationDto>> confirm(
            @PathVariable UUID id,
            @Valid @RequestBody ReviewViolationRequest request,
            @CurrentUser UserPrincipal principal) {
        ViolationDto dto = violationService.confirm(id, request, principal);
        return ResponseEntity.ok(ApiResponse.ok("Violation confirmed — fine issuance triggered", dto));
    }

    @PostMapping("/{id}/dismiss")
    @Operation(summary = "Dismiss a violation as invalid (OFFICER/ADMIN)",
            description = "Transitions PENDING or CONFIRMED → DISMISSED. No fine is issued.")
    @PreAuthorize("hasAnyRole('ADMIN', 'OFFICER')")
    public ResponseEntity<ApiResponse<ViolationDto>> dismiss(
            @PathVariable UUID id,
            @Valid @RequestBody ReviewViolationRequest request,
            @CurrentUser UserPrincipal principal) {
        ViolationDto dto = violationService.dismiss(id, request, principal);
        return ResponseEntity.ok(ApiResponse.ok("Violation dismissed", dto));
    }

    // ── scoped list endpoints ─────────────────────────────────────────────

    @GetMapping("/driver/{driverId}")
    @Operation(summary = "Get all violations for a specific driver")
    @PreAuthorize("hasAnyRole('ADMIN', 'OFFICER') or @securityHelper.isOwnDriverRecord(#driverId, principal)")
    public ResponseEntity<ApiResponse<List<ViolationDto>>> getForDriver(
            @PathVariable UUID driverId,
            @CurrentUser UserPrincipal principal) {
        List<ViolationDto> violations = violationService.getViolationsForDriver(driverId);
        return ResponseEntity.ok(ApiResponse.ok(violations));
    }

    @GetMapping("/vehicle/{vehicleId}")
    @Operation(summary = "Get all violations for a specific vehicle (OFFICER/ADMIN)")
    @PreAuthorize("hasAnyRole('ADMIN', 'OFFICER')")
    public ResponseEntity<ApiResponse<List<ViolationDto>>> getForVehicle(
            @PathVariable UUID vehicleId) {
        List<ViolationDto> violations = violationService.getViolationsForVehicle(vehicleId);
        return ResponseEntity.ok(ApiResponse.ok(violations));
    }

    @GetMapping("/pending")
    @Operation(summary = "Get all violations awaiting officer review (OFFICER/ADMIN)")
    @PreAuthorize("hasAnyRole('ADMIN', 'OFFICER')")
    public ResponseEntity<ApiResponse<List<ViolationDto>>> getPendingReview() {
        ViolationSearchObject search = new ViolationSearchObject();
        search.setStatus(ViolationStatus.PENDING);
        search.setLimit(100);
        return ResponseEntity.ok(ApiResponse.ok(violationService.search(search).getResultList()));
    }

    @GetMapping("/my")
    @Operation(summary = "Get all violations for the current authenticated citizen")
    public ResponseEntity<ApiResponse<List<ViolationDto>>> getMyViolations(@CurrentUser UserPrincipal principal) {
        UUID driverId = securityHelper.resolveDriverId(principal);
        List<ViolationDto> violations = violationService.getViolationsForDriver(driverId);
        return ResponseEntity.ok(ApiResponse.ok(violations));
    }
}