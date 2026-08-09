package com.academy.trafficviolationsystem.payment;

import com.academy.trafficviolationsystem.fine.FineEntity;

import java.util.UUID;

/**
 * Published by PaymentService after a payment transitions to SUCCESS.
 *
 * Carries only IDs (not the managed entities) — by the time this event is
 * handled (AFTER_COMMIT, on a listener thread) the original PaymentService
 * transaction/persistence context is closed, so the listener must reload
 * fresh entities via its own repository calls rather than reuse detached
 * instances.
 *
 * Why an event instead of calling PaymentConfirmationPdfService directly:
 * PaymentConfirmationPdfService.generateReceipt() is @Async, which schedules
 * execution on a separate thread immediately — it does NOT wait for the
 * caller's transaction to commit. Calling it directly from inside
 * PaymentService.pay() (which is @Transactional) creates a race where the
 * async thread's UPDATE can run against a payment row that isn't committed
 * yet, silently matching zero rows. Publishing this event and listening for
 * it at AFTER_COMMIT guarantees the payment row is durable before any PDF
 * work starts.
 */
public record PaymentSucceededEvent(UUID paymentId, UUID fineId) {

    public PaymentSucceededEvent(PaymentEntity payment, FineEntity fine) {
        this(payment.getId(), fine.getId());
    }
}
