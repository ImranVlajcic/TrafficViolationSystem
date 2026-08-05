package com.academy.trafficviolationsystem.core.exceptions.violations;

import com.academy.trafficviolationsystem.core.exceptions.AppException;
import com.academy.trafficviolationsystem.core.exceptions.ErrorCode;
import org.springframework.http.HttpStatus;

public class ViolationAlreadyDismissedException extends AppException {
    public ViolationAlreadyDismissedException(Object id) {
        super(HttpStatus.CONFLICT, ErrorCode.VIOLATION_ALREADY_DISMISSED,
                "Violation " + id + " has been dismissed and cannot be modified");
    }
}
