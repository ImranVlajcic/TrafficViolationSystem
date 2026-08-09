package com.academy.trafficviolationsystem.payment;

import com.academy.trafficviolationsystem.core.annotations.CurrentUser;
import com.academy.trafficviolationsystem.core.controllers.BaseController;
import com.academy.trafficviolationsystem.core.model.ApiResponse;
import com.academy.trafficviolationsystem.core.model.PagedResult;
import com.academy.trafficviolationsystem.core.security.UserPrincipal;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
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
 * REST controller for payment processing and history.
 * Mapped to /api/payments.
 *
 * Implements BaseController (read-only base — no generic POST/PUT from
 * BaseCRUDController, because payment creation goes through pay() which
 * invokes the gateway simulator, not a generic mapper-based insert).
 *
 * Endpoints:
 *   GET  /api/payments              → search (ADMIN/OFFICER only — see note on search() override)
 *   GET  /api/payments/{id}         → findById (any authenticated user; citizens limited to own payments)
 *   POST /api/payments              → pay a fine (all authenticated roles)
 *   GET  /api/payments/{id}/receipt → stream receipt PDF
 *   GET  /api/payments/fine/{fineId}→ all attempts for a fine (OFFICER/ADMIN)
 *   GET  /api/payments/my           → citizen's own payment history
 */
@RestController
@RequestMapping("/api/payments")
@Tag(name = "Payments", description = "Fine payment processing and receipt management")
public class PaymentController implements BaseController<
        PaymentEntity, PaymentDto, PaymentSearchObject, UUID> {

    private final PaymentService paymentService;

    public PaymentController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    @Override
    public PaymentService getService() {
        return paymentService;
    }

    // ── pay a fine ────────────────────────────────────────────────────────

    @PostMapping
    @Operation(
            summary = "Pay a fine",
            description = "Submits a payment against a fine. Amount is taken from fine.totalDue — " +
                    "never from the request body. Returns the transaction outcome immediately. " +
                    "Receipt PDF is generated asynchronously — poll receiptReady on the DTO."
    )
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<PaymentDto>> pay(
            @Valid @RequestBody PaymentRequest request,
            @CurrentUser UserPrincipal principal) {

        PaymentDto dto = paymentService.pay(request, principal);

        HttpStatus httpStatus = dto.getStatus() == PaymentStatus.SUCCESS
                ? HttpStatus.CREATED
                : HttpStatus.OK;

        String message = dto.getStatus() == PaymentStatus.SUCCESS
                ? "Payment successful — receipt will be available shortly"
                : "Payment " + dto.getStatus().name().toLowerCase();

        return ResponseEntity.status(httpStatus)
                .body(ApiResponse.ok(message, dto));
    }

    // ── receipt PDF download ──────────────────────────────────────────────

    @GetMapping("/{id}/receipt")
    @Operation(
            summary = "Download the payment receipt PDF",
            description = "Returns HTTP 404 if the PDF is not yet ready. " +
                    "Check receiptReady = true on the payment DTO before calling this endpoint."
    )
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Resource> downloadReceipt(
            @PathVariable UUID id,
            @CurrentUser UserPrincipal principal) {

        PaymentEntity payment = paymentService.findEntityById(id);

        // Authorisation: citizens can only download their own receipts
        if (principal.isCitizen()
                && payment.getPaidBy() != null
                && !payment.getPaidBy().getId().equals(principal.getId())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        if (payment.getReceiptPdfPath() == null) {
            return ResponseEntity.notFound().build();
        }

        File file = new File(payment.getReceiptPdfPath());
        if (!file.exists()) {
            return ResponseEntity.notFound().build();
        }

        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"receipt-" + payment.getTransactionId() + ".pdf\"")
                .body(new FileSystemResource(file));
    }

    // ── generic list/find — restricted, see Authorisation note below ───────

    /**
     * SECURITY FIX: BaseController's default findById()/search() previously
     * had no role restriction on this controller (no class-level
     * @PreAuthorize, no override here), unlike every other scoped endpoint
     * in this class. That left GET /api/payments and GET /api/payments/{id}
     * reachable by any authenticated user, including CITIZEN — e.g. a
     * citizen could pass ?paidById=<someone-else> and read another driver's
     * transaction ID, gateway response, and notes.
     *
     * search() is now ADMIN/OFFICER only — citizens use GET /api/payments/my
     * instead, which force-scopes the query to their own paidById.
     *
     * findById() stays open to all authenticated users but adds the same
     * ownership check already used by downloadReceipt(): a CITIZEN may only
     * fetch a payment they made themselves.
     *
     * Return types mirror BaseController's default methods exactly (DTO /
     * PagedResult<DTO>, not ResponseEntity<ApiResponse<...>>) — the
     * ApiResponse wrapping for these two endpoints happens outside this
     * class (a ResponseBodyAdvice), unlike the hand-written endpoints below
     * which wrap explicitly themselves.
     */
    @Override
    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'OFFICER')")
    public PagedResult<PaymentDto> search(@ParameterObject PaymentSearchObject searchObj) {
        return paymentService.search(searchObj);
    }

    @Override
    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public PaymentDto findById(@PathVariable UUID id) {
        PaymentEntity payment = paymentService.findEntityById(id);

        UserPrincipal principal = (UserPrincipal)
                SecurityContextHolder.getContext().getAuthentication().getPrincipal();

        if (principal.isCitizen()
                && payment.getPaidBy() != null
                && !payment.getPaidBy().getId().equals(principal.getId())) {
            throw new AccessDeniedException("You may only view your own payments");
        }

        return paymentService.toDtoWithFineNumber(payment);
    }

    // ── scoped list endpoints ─────────────────────────────────────────────

    @GetMapping("/fine/{fineId}")
    @Operation(summary = "Get all payment attempts for a specific fine (OFFICER/ADMIN)")
    @PreAuthorize("hasAnyRole('ADMIN', 'OFFICER')")
    public ResponseEntity<ApiResponse<List<PaymentDto>>> getForFine(
            @PathVariable UUID fineId) {
        return ResponseEntity.ok(
                ApiResponse.ok(paymentService.getPaymentsForFine(fineId)));
    }

    @GetMapping("/my")
    @Operation(summary = "Get the current citizen's own payment history")
    @PreAuthorize("hasRole('CITIZEN')")
    public ResponseEntity<ApiResponse<PagedResult<PaymentDto>>> getMyPayments(
            @ParameterObject PaymentSearchObject searchObj,
            @CurrentUser UserPrincipal principal) {

        // Scope search to the authenticated user's payments only
        searchObj.setPaidById(principal.getId());
        return ResponseEntity.ok(
                ApiResponse.ok(paymentService.search(searchObj)));
    }
}