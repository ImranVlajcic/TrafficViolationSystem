package com.academy.trafficviolationsystem.core.exceptions.fine;

import com.academy.trafficviolationsystem.core.exceptions.AppException;
import com.academy.trafficviolationsystem.core.exceptions.ErrorCode;
import org.springframework.http.HttpStatus;

public class FineAlreadyCancelledException extends AppException {
    public FineAlreadyCancelledException(Object id) {
        super(HttpStatus.CONFLICT, ErrorCode.FINE_ALREADY_CANCELLED,
                "Fine " + id + " is already cancelled");
    }
}
