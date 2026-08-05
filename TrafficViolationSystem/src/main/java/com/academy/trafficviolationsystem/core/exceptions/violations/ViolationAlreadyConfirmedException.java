package com.academy.trafficviolationsystem.core.exceptions.violations;

import com.academy.trafficviolationsystem.core.exceptions.AppException;
import com.academy.trafficviolationsystem.core.exceptions.ErrorCode;
import org.springframework.http.HttpStatus;

public class ViolationAlreadyConfirmedException extends AppException {
    public ViolationAlreadyConfirmedException(Object id) {
        super(HttpStatus.CONFLICT, ErrorCode.VIOLATION_ALREADY_CONFIRMED,
                "Violation " + id + " is already confirmed");
    }
    public ViolationAlreadyConfirmedException(String message) {
        super(HttpStatus.CONFLICT, ErrorCode.VIOLATION_ALREADY_CONFIRMED, message);
    }
}
