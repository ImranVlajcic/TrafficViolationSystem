package com.academy.trafficviolationsystem.audit;

import com.academy.trafficviolationsystem.core.controllers.BaseController;
import com.academy.trafficviolationsystem.core.model.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

/**
 * REST controller for audit log access.
 * Mapped to /api/audit — ADMIN only.
 *
 * Implements BaseController (read-only base):
 *   GET /api/audit           → search(AuditSearchObject) → PagedResult<AuditLogDto>
 *   GET /api/audit/{id}      → findById(UUID)            → AuditLogDto
 *
 * Extra endpoints:
 *   GET /api/audit/entity/{type}/{entityId} — full history for one record
 *   GET /api/audit/actor/{userId}           — all actions by a user
 *
 * No POST/PUT/DELETE — the audit log is append-only and written by AuditAspect.
 * Even admins cannot modify or delete audit entries.
 */
@RestController
@RequestMapping("/api/audit")
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Audit Log", description = "Immutable system audit trail — ADMIN only")
public class AuditController implements BaseController<
        AuditLogEntity, AuditLogDto, AuditSearchObject, UUID> {

    private final AuditLogService auditLogService;

    public AuditController(AuditLogService auditLogService) {
        this.auditLogService = auditLogService;
    }

    @Override
    public AuditLogService getService() {
        return auditLogService;
    }

    // ── entity history ────────────────────────────────────────────────────

    @GetMapping("/entity/{type}/{entityId}")
    @Operation(
        summary = "Get full audit history for a specific record",
        description = "Returns every action ever performed on the given entity record. " +
                      "type is the simple class name, e.g. 'FineEntity', 'ViolationEntity'. " +
                      "Example: GET /api/audit/entity/FineEntity/{fineId}"
    )
    public ResponseEntity<ApiResponse<List<AuditLogDto>>> getForEntity(
            @PathVariable String type,
            @PathVariable UUID entityId) {
        List<AuditLogDto> history = auditLogService.getForEntity(type, entityId);
        return ResponseEntity.ok(ApiResponse.ok(history));
    }

    // ── actor history ─────────────────────────────────────────────────────

    @GetMapping("/actor/{userId}")
    @Operation(
        summary = "Get all audit entries for actions performed by a specific user",
        description = "Returns the complete action history of an officer or admin. " +
                      "Useful for accountability reviews and investigating suspicious activity."
    )
    public ResponseEntity<ApiResponse<List<AuditLogDto>>> getForActor(
            @PathVariable UUID userId) {
        List<AuditLogDto> actions = auditLogService.getForActor(userId);
        return ResponseEntity.ok(ApiResponse.ok(actions));
    }
}
