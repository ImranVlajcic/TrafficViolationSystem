package com.academy.trafficviolationsystem.camera;

import com.academy.trafficviolationsystem.core.annotations.CurrentUser;
import com.academy.trafficviolationsystem.core.controllers.BaseCRUDController;
import com.academy.trafficviolationsystem.core.model.ApiResponse;
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
 * REST controller for camera and radar device management.
 * Mapped to /api/cameras.
 *
 * Inherits from BaseCRUDController (free):
 *   GET  /api/cameras              → search(CameraSearchObject) → PagedResult<CameraDto>
 *   GET  /api/cameras/{id}         → findById(Integer)          → CameraDto
 *   POST /api/cameras              → create(CameraCreateRequest)→ CameraDto
 *   PUT  /api/cameras/{id}         → update(Integer, CameraUpdateRequest) → CameraDto
 *
 * Extra endpoints:
 *   GET  /api/cameras/{id}/events                          → event history
 *   GET  /api/cameras/{id}/maintenance                     → maintenance history
 *   POST /api/cameras/{id}/maintenance                     → log maintenance visit
 *   POST /api/cameras/{id}/maintenance/{logId}/complete    → mark scheduled entry done
 *   GET  /api/cameras/offline                              → all currently offline cameras
 *   DELETE /api/cameras/{id}                               → decommission (soft-delete)
 */
@RestController
@RequestMapping("/api/cameras")
@Tag(name = "Cameras & Radars", description = "Traffic monitoring device management — ADMIN/OFFICER")
@PreAuthorize("hasAnyRole('ADMIN', 'OFFICER')")
public class CameraController implements BaseCRUDController<
        CameraEntity, CameraDto, CameraSearchObject, CameraCreateRequest, CameraUpdateRequest, Integer> {

    private final CameraService cameraService;

    public CameraController(CameraService cameraService) {
        this.cameraService = cameraService;
    }

    @Override
    public CameraService getService() {
        return cameraService;
    }

    // ── event history ─────────────────────────────────────────────────────

    @GetMapping("/{id}/events")
    @Operation(summary = "Get raw MQTT event history for a camera (newest first)")
    public ResponseEntity<ApiResponse<List<CameraEventDto>>> getEvents(
            @PathVariable Integer id) {
        return ResponseEntity.ok(ApiResponse.ok(cameraService.getEventHistory(id)));
    }

    // ── maintenance ───────────────────────────────────────────────────────

    @GetMapping("/{id}/maintenance")
    @Operation(summary = "Get maintenance history for a camera")
    public ResponseEntity<ApiResponse<List<CameraMaintenanceLogDto>>> getMaintenanceHistory(
            @PathVariable Integer id) {
        return ResponseEntity.ok(ApiResponse.ok(cameraService.getMaintenanceHistory(id)));
    }

    @PostMapping("/{id}/maintenance")
    @Operation(summary = "Log a maintenance visit for a camera (ADMIN only)")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<CameraMaintenanceLogDto>> logMaintenance(
            @PathVariable Integer id,
            @Valid @RequestBody LogMaintenanceRequest request,
            @CurrentUser UserPrincipal principal) {
        CameraMaintenanceLogDto dto = cameraService.logMaintenance(id, request, principal);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created("Maintenance logged", dto));
    }

    @PostMapping("/{id}/maintenance/{logId}/complete")
    @Operation(summary = "Mark a scheduled maintenance entry as completed (ADMIN only)")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<CameraMaintenanceLogDto>> completeMaintenance(
            @PathVariable Integer id,
            @PathVariable UUID logId,
            @CurrentUser UserPrincipal principal) {
        CameraMaintenanceLogDto dto = cameraService.completeMaintenance(id, logId, principal);
        return ResponseEntity.ok(ApiResponse.ok("Maintenance marked as completed", dto));
    }

    // ── status endpoints ──────────────────────────────────────────────────

    @GetMapping("/offline")
    @Operation(summary = "List all currently offline active cameras")
    public ResponseEntity<ApiResponse<List<CameraDto>>> getOffline() {
        return ResponseEntity.ok(ApiResponse.ok(cameraService.getOfflineCameras()));
    }

    // ── decommission ──────────────────────────────────────────────────────

    @DeleteMapping("/{id}")
    @Operation(summary = "Decommission a camera — soft-delete (ADMIN only)")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> decommission(@PathVariable Integer id) {
        CameraEntity camera = cameraService.findEntityById(id);
        cameraService.getRepository().delete(camera); // @PreRemove sets deletedAt
        return ResponseEntity.ok(ApiResponse.ok("Camera decommissioned", null));
    }
}
