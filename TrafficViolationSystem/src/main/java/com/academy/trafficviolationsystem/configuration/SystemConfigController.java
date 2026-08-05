package com.academy.trafficviolationsystem.configuration;

import com.academy.trafficviolationsystem.core.controllers.BaseCRUDController;
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

/**
 * REST controller for system configuration management.
 * Mapped to /api/config — ADMIN only.
 *
 * Implements BaseCRUDController which provides (free):
 *   GET  /api/config         → search(SystemConfigSearchObject) → PagedResult<SystemConfigDto>
 *   GET  /api/config/{id}    → findById(Integer)                → SystemConfigDto
 *   PUT  /api/config/{id}    → update(Integer, SystemConfigUpdateRequest) → SystemConfigDto
 *
 * POST /api/config is inherited from BaseCRUDController but the service's
 * beforeInsert() throws METHOD_NOT_ALLOWED immediately — rows are Flyway-seeded.
 *
 * DELETE is not wired — BaseCRUDController does not expose delete by default,
 * keeping config rows permanent.
 *
 * Extra endpoint:
 *   GET /api/config/category/{category} — list all keys in one group
 */
@RestController
@RequestMapping("/api/config")
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "System Config", description = "Runtime key-value configuration — ADMIN only")
public class SystemConfigController implements BaseCRUDController<
        SystemConfigEntity, SystemConfigDto,
        SystemConfigSearchObject,
        SystemConfigUpdateRequest, SystemConfigUpdateRequest,
        Integer> {

    private final SystemConfigService service;

    public SystemConfigController(SystemConfigService service) {
        this.service = service;
    }

    @Override
    public SystemConfigService getService() {
        return service;
    }

    // ── extra endpoint: list by category ─────────────────────────────────────

    @GetMapping("/category/{category}")
    @Operation(
        summary = "Get all config entries in a category",
        description = "category values: FINE, DRIVER, NOTIFICATION, PDF, MQTT"
    )
    public ResponseEntity<ApiResponse<List<SystemConfigDto>>> findByCategory(
            @PathVariable String category) {
        return ResponseEntity.ok(ApiResponse.ok(service.findByCategory(category)));
    }
}
