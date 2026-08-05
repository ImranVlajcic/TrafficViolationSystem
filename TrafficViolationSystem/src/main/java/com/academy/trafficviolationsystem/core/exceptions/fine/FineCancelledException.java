package com.academy.trafficviolationsystem.core.exceptions.fine;

import com.academy.trafficviolationsystem.core.exceptions.AppException;
import com.academy.trafficviolationsystem.core.exceptions.ErrorCode;
import org.springframework.http.HttpStatus;

public class FineCancelledException extends AppException {
    public FineCancelledException(Object id) {
        super(HttpStatus.CONFLICT, ErrorCode.FINE_CANCELLED,
                "Fine " + id + " has been cancelled and cannot be paid");
    }
}
