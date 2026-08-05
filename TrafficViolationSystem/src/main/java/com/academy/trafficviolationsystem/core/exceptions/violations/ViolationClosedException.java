package com.academy.trafficviolationsystem.core.exceptions.violations;

import com.academy.trafficviolationsystem.core.exceptions.AppException;
import com.academy.trafficviolationsystem.core.exceptions.ErrorCode;
import org.springframework.http.HttpStatus;

public class ViolationClosedException extends AppException {
    public ViolationClosedException(Object id) {
        super(HttpStatus.CONFLICT, ErrorCode.VIOLATION_CLOSED,
                "Violation " + id + " is closed and cannot be modified");
    }
}
