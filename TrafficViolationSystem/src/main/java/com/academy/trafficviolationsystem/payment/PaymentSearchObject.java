package com.academy.trafficviolationsystem.payment;

import com.academy.trafficviolationsystem.core.model.BaseSearchObject;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.util.UUID;

/**
 * Search/filter parameters for GET /api/payments.
 *
 * All fields are optional. Null fields are ignored in PaymentService.additionalFilter().
 *
 * Example requests:
 *   GET /api/payments?status=SUCCESS&fineId=<uuid>
 *   GET /api/payments?paidById=<uuid>&fromDate=2025-01-01
 *   GET /api/payments?method=CASH&includeCount=true
 */
@Getter
@Setter
public class PaymentSearchObject extends BaseSearchObject<UUID> {

    /** Filter to all payment attempts for a specific fine. */
    private UUID fineId;

    /** Filter by transaction outcome. */
    private PaymentStatus status;

    /** Filter by payment method. */
    private PaymentMethod method;

    /** Filter by the user who initiated the payment. */
    private UUID paidById;

    /** paidAt date range — start (inclusive). Null = no lower bound. */
    private LocalDate fromDate;

    /** paidAt date range — end (inclusive). Null = no upper bound. */
    private LocalDate toDate;
}
