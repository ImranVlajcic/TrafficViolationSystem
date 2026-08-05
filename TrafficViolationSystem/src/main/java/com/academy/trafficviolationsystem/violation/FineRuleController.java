package com.academy.trafficviolationsystem.violation;

import com.academy.trafficviolationsystem.core.controllers.BaseCRUDController;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST controller for fine rule management.
 * Mapped to /api/fine-rules — secured to ADMIN only in SecurityConfig.
 *
 * Inherits from BaseCRUDController (all endpoints free):
 *   GET  /api/fine-rules           → search(FineRuleSearchObject) → PagedResult<FineRuleDto>
 *   GET  /api/fine-rules/{id}      → findById(Integer)            → FineRuleDto
 *   POST /api/fine-rules           → create(FineRuleCreateRequest)→ FineRuleDto
 *   PUT  /api/fine-rules/{id}      → update(Integer, FineRuleUpdateRequest) → FineRuleDto
 *
 * No extra endpoints — fine rules have no domain operations beyond CRUD.
 * Deactivating a rule is done via PUT /api/fine-rules/{id} with isActive=false.
 */
@RestController
@RequestMapping("/api/fine-rules")
@Tag(name = "Fine Rules", description = "Configure fine amounts and penalty points per violation type — ADMIN only")
public class FineRuleController implements BaseCRUDController<
        FineRuleEntity, FineRuleDto, FineRuleSearchObject, FineRuleCreateRequest, FineRuleUpdateRequest, Integer> {

    private final FineRuleService fineRuleService;

    public FineRuleController(FineRuleService fineRuleService) {
        this.fineRuleService = fineRuleService;
    }

    @Override
    public FineRuleService getService() {
        return fineRuleService;
    }
}
