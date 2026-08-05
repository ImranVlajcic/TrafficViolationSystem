package com.academy.trafficviolationsystem.driver;

import com.academy.trafficviolationsystem.core.annotations.CurrentUser;
import com.academy.trafficviolationsystem.core.controllers.BaseCRUDController;
import com.academy.trafficviolationsystem.core.model.ApiResponse;
import com.academy.trafficviolationsystem.core.security.UserPrincipal;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * REST controller for driver management.
 * Mapped to /api/drivers.
 *
 * Inherits from BaseCRUDController (free):
 *   GET    /api/drivers            → search(DriverSearchObject) → PagedResult<DriverDto>
 *   GET    /api/drivers/{id}       → findById(UUID)              → DriverDto
 *   POST   /api/drivers            → create(DriverCreateRequest) → DriverDto
 *   PUT    /api/drivers/{id}       → update(UUID, DriverUpdateRequest) → DriverDto
 *
 * Extra endpoints defined here:
 *   GET    /api/drivers/{id}/points
 *   POST   /api/drivers/{id}/suspend
 *   POST   /api/drivers/{id}/lift-suspension
 *   GET    /api/drivers/{id}/suspensions
 *   POST   /api/drivers/{id}/link-user
 *   GET    /api/drivers/by-license/{licenseNumber}
 */
@RestController
@RequestMapping("/api/drivers")
@Tag(name = "Drivers", description = "Driver and license management")
public class DriverController implements BaseCRUDController<
        DriverEntity, DriverDto, DriverSearchObject, DriverCreateRequest, DriverUpdateRequest, UUID> {

private final DriverService driverService;

public DriverController(DriverService driverService) {
    this.driverService = driverService;
}

@Override
public DriverService getService() {
    return driverService;
}

// ── penalty points ───────────────────────────────────────────────────

@GetMapping("/{id}/points")
@Operation(summary = "Get the penalty point history for a driver (OFFICER/ADMIN)")
@PreAuthorize("hasAnyRole('ADMIN', 'OFFICER')")
public ResponseEntity<ApiResponse<List<DriverPointHistoryDto>>> getPointHistory(
        @PathVariable UUID id) {
    List<DriverPointHistoryDto> history = driverService.getPointHistory(id);
    return ResponseEntity.ok(ApiResponse.ok(history));
}

// ── suspension ────────────────────────────────────────────────────────

@PostMapping("/{id}/suspend")
@Operation(summary = "Suspend a driver's license (OFFICER/ADMIN)")
@PreAuthorize("hasAnyRole('ADMIN', 'OFFICER')")
public ResponseEntity<ApiResponse<LicenseSuspensionDto>> suspend(
        @PathVariable UUID id,
        @Valid @RequestBody SuspendDriverRequest request,
        @CurrentUser UserPrincipal principal) {
    LicenseSuspensionDto dto = driverService.suspend(id, request, principal);
    return ResponseEntity.ok(ApiResponse.ok("Driver suspended successfully", dto));
}

@PostMapping("/{id}/lift-suspension")
@Operation(summary = "Lift a driver's current suspension (OFFICER/ADMIN)")
@PreAuthorize("hasAnyRole('ADMIN', 'OFFICER')")
public ResponseEntity<ApiResponse<DriverDto>> liftSuspension(@PathVariable UUID id) {
    DriverDto dto = driverService.liftSuspension(id);
    return ResponseEntity.ok(ApiResponse.ok("Suspension lifted successfully", dto));
}

@GetMapping("/{id}/suspensions")
@Operation(summary = "Get the suspension history for a driver (OFFICER/ADMIN)")
@PreAuthorize("hasAnyRole('ADMIN', 'OFFICER')")
public ResponseEntity<ApiResponse<List<LicenseSuspensionDto>>> getSuspensionHistory(
        @PathVariable UUID id) {
    List<LicenseSuspensionDto> history = driverService.getSuspensionHistory(id);
    return ResponseEntity.ok(ApiResponse.ok(history));
}

// ── portal account link ──────────────────────────────────────────────

@PostMapping("/{id}/link-user")
@Operation(summary = "Link a citizen user account to this driver record (ADMIN)")
@PreAuthorize("hasRole('ADMIN')")
public ResponseEntity<ApiResponse<DriverDto>> linkUserAccount(
        @PathVariable UUID id,
        @RequestParam UUID userId) {
    DriverDto dto = driverService.linkUserAccount(id, userId);
    return ResponseEntity.ok(ApiResponse.ok("User account linked successfully", dto));
}

// ── lookup ────────────────────────────────────────────────────────────

@GetMapping("/by-license/{licenseNumber}")
@Operation(summary = "Find a driver by license number (OFFICER/ADMIN)")
@PreAuthorize("hasAnyRole('ADMIN', 'OFFICER')")
public ResponseEntity<ApiResponse<DriverDto>> findByLicenseNumber(
        @PathVariable String licenseNumber) {
    DriverDto dto = driverService.getMapper().toDto(
            driverService.findByLicenseNumber(licenseNumber));
    return ResponseEntity.ok(ApiResponse.ok(dto));
}
}