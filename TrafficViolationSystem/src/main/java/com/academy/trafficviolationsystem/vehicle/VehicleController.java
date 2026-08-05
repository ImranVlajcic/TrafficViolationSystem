package com.academy.trafficviolationsystem.vehicle;

import com.academy.trafficviolationsystem.core.annotations.CurrentUser;
import com.academy.trafficviolationsystem.core.controllers.BaseCRUDController;
import com.academy.trafficviolationsystem.core.model.ApiResponse;
import com.academy.trafficviolationsystem.core.security.SecurityHelper;
import com.academy.trafficviolationsystem.core.security.UserPrincipal;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

/**
 * REST controller for vehicle management.
 * Mapped to /api/vehicles.
 *
 * Inherits from BaseCRUDController (free):
 *   GET    /api/vehicles           → search(VehicleSearchObject) → PagedResult<VehicleDto>
 *   GET    /api/vehicles/{id}      → findById(UUID)              → VehicleDto
 *   POST   /api/vehicles           → create(VehicleCreateRequest)→ VehicleDto
 *   PUT    /api/vehicles/{id}      → update(UUID, VehicleUpdateRequest) → VehicleDto
 *
 * Extra endpoints defined here:
 *   GET    /api/vehicles/{id}/ownership-history
 *   POST   /api/vehicles/{id}/transfer-ownership
 *   POST   /api/vehicles/{id}/mark-stolen
 *   POST   /api/vehicles/{id}/mark-found
 *   GET    /api/vehicles/stolen
 *   GET    /api/vehicles/my
 *   DELETE /api/vehicles/{id}  → soft-delete (deregister)
 */
@RestController
@RequestMapping("/api/vehicles")
@Tag(name = "Vehicles", description = "Vehicle registration and management")
public class VehicleController implements BaseCRUDController<
        VehicleEntity, VehicleDto, VehicleSearchObject, VehicleCreateRequest, VehicleUpdateRequest, UUID> {

    private final VehicleService vehicleService;
    private final SecurityHelper securityHelper;

    public VehicleController(VehicleService vehicleService, SecurityHelper securityHelper) {
        this.vehicleService = vehicleService;
        this.securityHelper = securityHelper;
    }

    @Override
    public VehicleService getService() {
        return vehicleService;
    }

    // ── ownership history ─────────────────────────────────────────────────

    @GetMapping("/{id}/ownership-history")
    @Operation(summary = "Get the full ownership transfer history for a vehicle")
    public ResponseEntity<ApiResponse<List<VehicleOwnershipHistoryDto>>> getOwnershipHistory(
            @PathVariable UUID id) {
        List<VehicleOwnershipHistoryDto> history = vehicleService.getOwnershipHistory(id);
        return ResponseEntity.ok(ApiResponse.ok(history));
    }

    // ── ownership transfer ────────────────────────────────────────────────

    @PostMapping("/{id}/transfer-ownership")
    @Operation(summary = "Transfer vehicle ownership to a new driver (OFFICER/ADMIN)")
    @PreAuthorize("hasAnyRole('ADMIN', 'OFFICER')")
    public ResponseEntity<ApiResponse<VehicleDto>> transferOwnership(
            @PathVariable UUID id,
            @Valid @RequestBody TransferOwnershipRequest request) {
        VehicleDto dto = vehicleService.transferOwnership(id, request);
        return ResponseEntity.ok(ApiResponse.ok("Ownership transferred successfully", dto));
    }

    // ── stolen flag ───────────────────────────────────────────────────────

    @PostMapping("/{id}/mark-stolen")
    @Operation(summary = "Flag a vehicle as stolen (OFFICER/ADMIN)")
    @PreAuthorize("hasAnyRole('ADMIN', 'OFFICER')")
    public ResponseEntity<ApiResponse<VehicleDto>> markStolen(@PathVariable UUID id) {
        VehicleDto dto = vehicleService.markStolen(id);
        return ResponseEntity.ok(ApiResponse.ok("Vehicle marked as stolen", dto));
    }

    @PostMapping("/{id}/mark-found")
    @Operation(summary = "Clear the stolen flag when a vehicle is recovered (OFFICER/ADMIN)")
    @PreAuthorize("hasAnyRole('ADMIN', 'OFFICER')")
    public ResponseEntity<ApiResponse<VehicleDto>> markFound(@PathVariable UUID id) {
        VehicleDto dto = vehicleService.markFound(id);
        return ResponseEntity.ok(ApiResponse.ok("Vehicle marked as found", dto));
    }

    @GetMapping("/stolen")
    @Operation(summary = "List all currently stolen vehicles (OFFICER/ADMIN)")
    @PreAuthorize("hasAnyRole('ADMIN', 'OFFICER')")
    public ResponseEntity<ApiResponse<List<VehicleDto>>> getStolenVehicles() {
        List<VehicleDto> vehicles = vehicleService.getStolenVehicles();
        return ResponseEntity.ok(ApiResponse.ok(vehicles));
    }

    // ── current citizen's own vehicles ──────────────────────────────────────

    @GetMapping("/my")
    @Operation(summary = "Get all vehicles registered to the current authenticated citizen")
    public ResponseEntity<ApiResponse<List<VehicleDto>>> getMyVehicles(@CurrentUser UserPrincipal principal) {
        UUID driverId = securityHelper.resolveDriverId(principal);
        VehicleSearchObject search = new VehicleSearchObject();
        search.setOwnerId(driverId);
        search.setLimit(100);
        return ResponseEntity.ok(ApiResponse.ok(vehicleService.search(search).getResultList()));
    }

    // ── soft-delete (deregister) ──────────────────────────────────────────

    @DeleteMapping("/{id}")
    @Operation(summary = "Deregister a vehicle — soft-delete (ADMIN only)")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> deregister(@PathVariable UUID id) {
        VehicleEntity vehicle = vehicleService.findEntityById(id);
        vehicleService.getRepository().delete(vehicle); // @PreRemove sets deletedAt
        return ResponseEntity.status(HttpStatus.OK)
                .body(ApiResponse.ok("Vehicle deregistered successfully", null));
    }
}