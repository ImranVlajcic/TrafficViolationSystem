package com.academy.trafficviolationsystem.analytics;

import com.academy.trafficviolationsystem.core.annotations.CurrentUser;
import com.academy.trafficviolationsystem.core.controllers.BaseController;
import com.academy.trafficviolationsystem.core.model.ApiResponse;
import com.academy.trafficviolationsystem.core.security.UserPrincipal;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.File;
import java.util.List;
import java.util.UUID;

/**
 * REST controller for report generation and retrieval.
 * Mapped to /api/reports.
 *
 * Implements BaseController (read-only base) — creation goes through
 * POST /api/reports which calls ReportService.requestReport(), not a
 * generic insert().
 *
 * Endpoints:
 *   GET  /api/reports                  → search (ADMIN sees all, OFFICER sees all)
 *   GET  /api/reports/{id}             → always returns ReportDto JSON (poll status)
 *   GET  /api/reports/{id}/download    → stream file once isReady = true
 *   POST /api/reports                  → request async generation
 *   GET  /api/reports/my               → authenticated user's own reports
 *
 * The split of /{id} (JSON) and /{id}/download (file) fixes the dual content-type
 * anti-pattern where the same endpoint returned either JSON or a binary file
 * depending on state — that made frontend integration brittle.
 */
@RestController
@RequestMapping("/api/reports")
@Tag(name = "Reports", description = "On-demand report generation and download")
public class ReportController implements BaseController<
        GeneratedReportEntity, ReportDto, ReportSearchObject, UUID> {

    private final ReportService reportService;

    public ReportController(ReportService reportService) {
        this.reportService = reportService;
    }

    @Override
    public ReportService getService() {
        return reportService;
    }

    // ── Override findById to use ReportService.getReport() ───────────────────

    @Override
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('OFFICER', 'ADMIN')")
    @Operation(
        summary = "Get report status",
        description = "Always returns a ReportDto (JSON). " +
                      "Poll this until isReady = true, then call /{id}/download."
    )
    public ReportDto findById(@PathVariable UUID id) {
        return reportService.getReport(id);
    }

    // ── Request report generation ─────────────────────────────────────────────

    @PostMapping
    @PreAuthorize("hasAnyRole('OFFICER', 'ADMIN')")
    @Operation(
        summary = "Request a new report",
        description = "Creates a PENDING report and fires async generation. " +
                      "Returns the PENDING ReportDto immediately. " +
                      "Poll GET /api/reports/{id} until isReady = true."
    )
    public ResponseEntity<ApiResponse<ReportDto>> requestReport(
            @Valid @RequestBody ReportRequestDto request,
            @CurrentUser UserPrincipal principal) {
        ReportDto dto = reportService.requestReport(request, principal);
        return ResponseEntity.status(HttpStatus.ACCEPTED)
                .body(ApiResponse.ok("Report generation started", dto));
    }

    // ── Download ──────────────────────────────────────────────────────────────

    @GetMapping("/{id}/download")
    @PreAuthorize("hasAnyRole('OFFICER', 'ADMIN')")
    @Operation(
        summary = "Download a generated report file",
        description = "Streams the PDF or CSV file. " +
                      "Returns 404 if status is not DONE or if the file no longer exists on disk. " +
                      "Check isReady = true on the ReportDto before calling this endpoint."
    )
    public ResponseEntity<Resource> download(
            @PathVariable UUID id,
            @CurrentUser UserPrincipal principal) {

        GeneratedReportEntity report = reportService.getReportEntity(id);

        if (report.getStatus() != ReportStatus.DONE || report.getFilePath() == null) {
            return ResponseEntity.notFound().build();
        }

        File file = new File(report.getFilePath());
        if (!file.exists()) {
            return ResponseEntity.notFound().build();
        }

        String contentType = report.getFilePath().endsWith(".pdf")
                ? MediaType.APPLICATION_PDF_VALUE
                : "text/csv";

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"" + file.getName() + "\"")
                .contentType(MediaType.parseMediaType(contentType))
                .body(new FileSystemResource(file));
    }

    // ── My reports ────────────────────────────────────────────────────────────

    @GetMapping("/my")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Get the current user's own report history")
    public ResponseEntity<ApiResponse<List<ReportDto>>> getMyReports(
            @CurrentUser UserPrincipal principal) {
        return ResponseEntity.ok(
                ApiResponse.ok(reportService.getMyReports(principal)));
    }
}
