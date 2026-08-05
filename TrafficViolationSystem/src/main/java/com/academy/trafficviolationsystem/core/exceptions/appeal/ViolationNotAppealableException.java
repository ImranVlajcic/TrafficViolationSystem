package com.academy.trafficviolationsystem.core.exceptions.appeal;

import com.academy.trafficviolationsystem.core.exceptions.AppException;
import com.academy.trafficviolationsystem.core.exceptions.ErrorCode;
import com.academy.trafficviolationsystem.violation.ViolationStatus;
import org.springframework.http.HttpStatus;

public class ViolationNotAppealableException extends AppException {
    public ViolationNotAppealableException(ViolationStatus currentStatus) {
        super(HttpStatus.CONFLICT, ErrorCode.VIOLATION_NOT_APPEALABLE,
                "Appeals can only be filed for violations in CONFIRMED or DISPUTED status. " +
                        "Current status: " + currentStatus);
    }
}
