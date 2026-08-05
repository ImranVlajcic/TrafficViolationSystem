package com.academy.trafficviolationsystem.payment;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Read-only projection of PaymentEntity returned by all payment endpoints.
 *
 * receiptReady is a computed field set by PaymentMapper @AfterMapping.
 * fineNumber is populated by PaymentService after a FineRepository lookup —
 * it is not mapped by MapStruct directly.
 */
@Getter
@Setter
public class PaymentDto {

    private UUID id;
    private String transactionId;
    private BigDecimal amount;
    private String currency;
    private PaymentMethod method;
    private PaymentStatus status;
    private LocalDateTime paidAt;
    private String gatewayResponse;
    private String receiptPdfPath;
    private String notes;

    /**
     * Computed by PaymentMapper @AfterMapping.
     * True when receiptPdfPath is non-null (PDF generation completed).
     */
    private boolean receiptReady;

    // fine summary
    private UUID fineId;

    /**
     * Populated by PaymentService.toDtoWithFineNumber() — not set by MapStruct
     * because it requires a separate FineRepository lookup.
     */
    private String fineNumber;

    // payer summary
    private UUID paidById;
    private String paidByUsername;

    // audit
    private Instant created;
}
