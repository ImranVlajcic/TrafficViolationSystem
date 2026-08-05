package com.academy.trafficviolationsystem.core.exceptions.fine;

import com.academy.trafficviolationsystem.core.exceptions.AppException;
import com.academy.trafficviolationsystem.core.exceptions.ErrorCode;
import org.springframework.http.HttpStatus;

public class FineAlreadyPaidException extends AppException {
    public FineAlreadyPaidException(Object id) {
        super(HttpStatus.CONFLICT, ErrorCode.FINE_ALREADY_PAID,
                "Fine " + id + " has already been paid");
    }
}
