package com.academy.trafficviolationsystem.rodezone;

import com.academy.trafficviolationsystem.core.annotations.CurrentUser;
import com.academy.trafficviolationsystem.core.controllers.BaseCRUDController;
import com.academy.trafficviolationsystem.core.model.ApiResponse;
import com.academy.trafficviolationsystem.core.security.UserPrincipal;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * REST controller for road zone management.
 * Mapped to /api/zones.
 *
 * Implements BaseCRUDController which provides (free):
 *   GET  /api/zones           → search(RoadZoneSearchObject) → PagedResult<RoadZoneDto>
 *   GET  /api/zones/{id}      → findById(Integer)            → RoadZoneDto
 *   POST /api/zones           → create(RoadZoneCreateRequest)→ RoadZoneDto  [ADMIN only]
 *   PUT  /api/zones/{id}      → update(Integer, RoadZoneUpdateRequest) → RoadZoneDto [ADMIN only]
 *
 * Note: BaseCRUDController inherits create() and update() — role guards are applied
 * using @PreAuthorize at the method level below by overriding and re-annotating.
 *
 * Extra endpoints:
 *   GET    /api/zones/active                        — flat list for map / dropdowns
 *   POST   /api/zones/{id}/assign-camera/{cameraId} — link camera to zone [ADMIN]
 *   DELETE /api/zones/unassign-camera/{cameraId}    — remove camera from zone [ADMIN]
 *
 * PATCH removed — PUT handles partial updates via RoadZoneUpdateRequest with
 * IGNORE null-value strategy in the mapper, consistent with the rest of the codebase.
 */
@RestController
@RequestMapping("/api/zones")
@Tag(name = "Road Zones", description = "Road zone management")
public class RoadZoneController implements BaseCRUDController<
        RoadZoneEntity, RoadZoneDto,
        RoadZoneSearchObject,
        RoadZoneCreateRequest, RoadZoneUpdateRequest,
        Integer> {

    private final RoadZoneService service;

    public RoadZoneController(RoadZoneService service) {
        this.service = service;
    }

    @Override
    public RoadZoneService getService() {
        return service;
    }

    // ── Role-guarded write overrides ──────────────────────────────────────────

    /**
     * POST /api/zones — ADMIN only.
     * Inherited from BaseCRUDController and re-annotated with role guard.
     */
    @Override
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Create a new road zone (ADMIN only)")
    public RoadZoneDto create(
            @org.springframework.web.bind.annotation.RequestBody
            @jakarta.validation.Valid RoadZoneCreateRequest request) {
        return BaseCRUDController.super.create(request);
    }

    /**
     * PUT /api/zones/{id} — ADMIN only.
     * All fields in RoadZoneUpdateRequest are optional (IGNORE null-value strategy
     * in mapper), so this acts as a partial update — no PATCH needed.
     */
    @Override
    @org.springframework.web.bind.annotation.PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Update a road zone (ADMIN only)")
    public RoadZoneDto update(
            @PathVariable Integer id,
            @org.springframework.web.bind.annotation.RequestBody
            @jakarta.validation.Valid RoadZoneUpdateRequest request) {
        return BaseCRUDController.super.update(id, request);
    }

    // ── Extra endpoints ───────────────────────────────────────────────────────

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Delete a road zone — soft-delete, clears camera assignments (ADMIN only)")
    public ResponseEntity<ApiResponse<Void>> deleteZone(@PathVariable Integer id) {
        service.deleteZone(id);
        return ResponseEntity.ok(ApiResponse.ok("Zone deleted successfully", null));
    }

    @GetMapping("/active")
    @Operation(summary = "Get all active zones — for map layer and camera-assignment dropdowns")
    public ResponseEntity<ApiResponse<List<RoadZoneDto>>> findActiveZones() {
        return ResponseEntity.ok(ApiResponse.ok(service.findActiveZones()));
    }

    @PostMapping("/{id}/assign-camera/{cameraId}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Assign a camera to a zone (ADMIN only)")
    public ResponseEntity<ApiResponse<Void>> assignCamera(
            @PathVariable Integer id,
            @PathVariable Integer cameraId) {
        service.assignCameraToZone(id, cameraId);
        return ResponseEntity.ok(ApiResponse.ok("Camera " + cameraId + " assigned to zone " + id, null));
    }

    @DeleteMapping("/unassign-camera/{cameraId}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Remove a camera from its zone (ADMIN only)")
    public ResponseEntity<ApiResponse<Void>> unassignCamera(
            @PathVariable Integer cameraId) {
        service.unassignCameraFromZone(cameraId);
        return ResponseEntity.ok(ApiResponse.ok("Camera " + cameraId + " unassigned from zone", null));
    }
}
