package com.academy.trafficviolationsystem.payment;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

/**
 * Request body for POST /api/payments.
 *
 * The amount is intentionally absent — PaymentService reads fine.totalDue
 * at the moment of payment and uses that figure. Clients never submit the
 * amount directly, which prevents manipulation (e.g. sending amount=0.01).
 *
 * For CREDIT_CARD and DEBIT_CARD, the card details would be collected by
 * the frontend and passed directly to the real payment gateway in production.
 * In this simulated version the gateway outcome is determined by
 * PaymentGatewaySimulator based on simple rules — no card data needed here.
 */
@Getter
@Setter
public class PaymentRequest {

    @NotNull(message = "Fine ID is required")
    private UUID fineId;

    @NotBlank(message = "Idempotency key is required")
    private String idempotencyKey;

    @NotNull(message = "Payment method is required")
    private PaymentMethod method;

    /**
     * Optional reference or cashier note.
     * For BANK_TRANSFER: the bank reference number the payer used.
     * For CASH: the cashier's terminal ID or receipt number.
     * For card payments: null.
     */
    private String notes;
}
