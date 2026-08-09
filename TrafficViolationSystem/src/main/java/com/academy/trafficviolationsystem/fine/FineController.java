package com.academy.trafficviolationsystem.fine;

import com.academy.trafficviolationsystem.core.annotations.CurrentUser;
import com.academy.trafficviolationsystem.core.controllers.BaseController;
import com.academy.trafficviolationsystem.core.model.ApiResponse;
import com.academy.trafficviolationsystem.core.model.PagedResult;
import com.academy.trafficviolationsystem.core.security.UserPrincipal;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.NotBlank;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.File;
import java.util.List;
import java.util.UUID;

/**
 * REST controller for fine management.
 * Mapped to /api/fines.
 *
 * Implements BaseController (read-only base — no generic POST/PUT):
 *   GET  /api/fines         → search(FineSearchObject) → PagedResult<FineDto>
 *   GET  /api/fines/{id}    → getFineWithDetails(UUID) → FineDto
 *
 * Extra endpoints:
 *   POST /api/fines/{id}/cancel         → OFFICER/ADMIN cancel a fine
 *   GET  /api/fines/{id}/pdf            → stream the fine PDF document
 *   GET  /api/fines/driver/{driverId}   → all fines for a driver
 *   GET  /api/fines/my                  → citizen's own fines
 *
 * Fines are never created via HTTP POST — they are created automatically
 * by FineService.onViolationConfirmed() when a violation is confirmed.
 */
@RestController
@RequestMapping("/api/fines")
@Tag(name = "Fines", description = "Fine management and PDF document access")
public class FineController implements BaseController<FineEntity, FineDto, FineSearchObject, UUID> {

    private final FineService fineService;

    public FineController(FineService fineService) {
        this.fineService = fineService;
    }

    @Override
    public FineService getService() {
        return fineService;
    }

    // ── override findById to return enriched DTO with violation reference ─

    // NOTE: this is a class override of an interface method (BaseController
    // .findById). If BaseController's default carries @PreAuthorize, the
    // CGLIB proxy does NOT apply it to this override — same confirmed bug
    // as the Appeal module. @PreAuthorize is restated explicitly here
    // (kept to the original (UUID id) signature — can't add a @CurrentUser
    // param without breaking @Override — so it uses Spring Security's
    // implicit `authentication` SpEL variable instead of #principal, same
    // as downloadPdf's SpEL below). Adjust roles/SpEL to match whatever
    // access rule BaseController's default actually encodes (this assumes:
    // staff can view any fine, citizens only their own).
    @Override
    @GetMapping("/{id}")
    @Operation(summary = "Get a fine by ID with full details including violation reference")
    @PreAuthorize("hasAnyRole('ADMIN', 'OFFICER') or @securityHelper.isOwnFine(#id, authentication.principal)")
    public FineDto findById(@PathVariable UUID id) {
        return fineService.getFineWithDetails(id);
    }

    // ── cancel ────────────────────────────────────────────────────────────

    @PostMapping("/{id}/cancel")
    @Operation(summary = "Cancel a fine (OFFICER/ADMIN)",
               description = "Reverses penalty points applied at issuance. Cannot cancel a paid fine.")
    @PreAuthorize("hasAnyRole('ADMIN', 'OFFICER')")
    public ResponseEntity<ApiResponse<FineDto>> cancel(
            @PathVariable UUID id,
            @RequestParam @NotBlank String reason,
            @CurrentUser UserPrincipal principal) {
        FineDto dto = fineService.cancel(id, reason, principal);
        return ResponseEntity.ok(ApiResponse.ok("Fine cancelled and penalty points reversed", dto));
    }

    // ── PDF download ──────────────────────────────────────────────────────

    @GetMapping("/{id}/pdf")
    @Operation(summary = "Download the official fine PDF document",
               description = "Returns 404 if the PDF is not yet generated (check pdfReady on the fine DTO first).")
    @PreAuthorize("hasAnyRole('ADMIN', 'OFFICER') or @securityHelper.isOwnFine(#id, principal)")
    public ResponseEntity<Resource> downloadPdf(
            @PathVariable UUID id,
            @CurrentUser UserPrincipal principal) {

        FineEntity fine = fineService.findEntityById(id);

        if (fine.getPdfPath() == null) {
            return ResponseEntity.notFound().build();
        }

        File pdfFile = new File(fine.getPdfPath());
        if (!pdfFile.exists()) {
            return ResponseEntity.notFound().build();
        }

        Resource resource = new FileSystemResource(pdfFile);
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"" + fine.getFineNumber() + ".pdf\"")
                .body(resource);
    }

    // ── scoped list endpoints ─────────────────────────────────────────────

    @GetMapping("/driver/{driverId}")
    @Operation(summary = "Get all fines for a specific driver (OFFICER/ADMIN)")
    @PreAuthorize("hasAnyRole('ADMIN', 'OFFICER')")
    public ResponseEntity<ApiResponse<List<FineDto>>> getForDriver(@PathVariable UUID driverId) {
        return ResponseEntity.ok(ApiResponse.ok(fineService.getForDriver(driverId)));
    }

    @GetMapping("/my")
    @Operation(summary = "Get the currently authenticated citizen's own fines")
    @PreAuthorize("hasRole('CITIZEN')")
    public ResponseEntity<ApiResponse<List<FineDto>>> getMyFines(
            @CurrentUser UserPrincipal principal) {
        return ResponseEntity.ok(ApiResponse.ok(fineService.getMyFines(principal.getId())));
    }
}
