package com.academy.trafficviolationsystem.payment;

import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Bridges PaymentService's transactional payment flow to the async receipt
 * PDF pipeline.
 *
 * Listening at TransactionPhase.AFTER_COMMIT guarantees the PaymentEntity
 * row (and the fine status update from fineService.markPaid()) are durably
 * committed before PaymentConfirmationPdfService starts work on its own
 * thread/session. Without this indirection, the @Async PDF call could start
 * before the originating transaction commits, causing its
 * setReceiptPdfPath() update to silently match zero rows.
 *
 * If the transaction rolls back for any reason, this listener never fires,
 * so we never generate a receipt for a payment that didn't actually succeed.
 */
@Component
public class PaymentEventListener {

    private final PaymentConfirmationPdfService pdfService;

    public PaymentEventListener(PaymentConfirmationPdfService pdfService) {
        this.pdfService = pdfService;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onPaymentSucceeded(PaymentSucceededEvent event) {
        pdfService.generateReceipt(event.paymentId(), event.fineId());
    }
}
