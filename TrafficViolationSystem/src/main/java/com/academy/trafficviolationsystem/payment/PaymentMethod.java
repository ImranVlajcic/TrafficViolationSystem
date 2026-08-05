package com.academy.trafficviolationsystem.payment;

/**
 * Payment method chosen by the payer.
 * Stored as STRING on PaymentEntity.
 * Used in PaymentRequest and PaymentSearchObject for filtering.
 */
public enum PaymentMethod {

    /** Physical credit card payment at a terminal or online card entry. */
    CREDIT_CARD,

    /** Physical debit card payment. */
    DEBIT_CARD,

    /** Bank wire transfer — payer provides a reference number. */
    BANK_TRANSFER,

    /** Cash payment at an authorised payment centre. */
    CASH,

    /** Payment initiated through the citizen web portal. */
    ONLINE_PORTAL
}
