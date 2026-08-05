package com.academy.trafficviolationsystem.core.exceptions.fine;

import com.academy.trafficviolationsystem.core.exceptions.AppException;
import com.academy.trafficviolationsystem.core.exceptions.ErrorCode;
import org.springframework.http.HttpStatus;

public class ViolationMissingDriverException extends AppException {
    public ViolationMissingDriverException(Object referenceNumber) {
        super(HttpStatus.CONFLICT, ErrorCode.VIOLATION_MISSING_DRIVER,
                "Cannot issue a fine for violation " + referenceNumber + " — no driver is assigned");
    }
}
