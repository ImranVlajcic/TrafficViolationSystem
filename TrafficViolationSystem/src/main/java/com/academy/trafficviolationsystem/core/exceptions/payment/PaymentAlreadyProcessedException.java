package com.academy.trafficviolationsystem.core.exceptions.payment;

import com.academy.trafficviolationsystem.core.exceptions.AppException;
import com.academy.trafficviolationsystem.core.exceptions.ErrorCode;
import org.springframework.http.HttpStatus;

public class PaymentAlreadyProcessedException extends AppException {
    public PaymentAlreadyProcessedException(String transactionId) {
        super(HttpStatus.CONFLICT, ErrorCode.PAYMENT_ALREADY_PROCESSED,
                "Transaction " + transactionId + " has already been processed");
    }
}
