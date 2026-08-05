package com.academy.trafficviolationsystem.core.exceptions.violations;

import com.academy.trafficviolationsystem.core.exceptions.AppException;
import com.academy.trafficviolationsystem.core.exceptions.ErrorCode;
import org.springframework.http.HttpStatus;

public class ViolationAlreadyHasFineException extends AppException {
    public ViolationAlreadyHasFineException(Object id) {
        super(HttpStatus.CONFLICT, ErrorCode.VIOLATION_ALREADY_HAS_FINE,
                "A fine already exists for violation " + id);
    }
}
