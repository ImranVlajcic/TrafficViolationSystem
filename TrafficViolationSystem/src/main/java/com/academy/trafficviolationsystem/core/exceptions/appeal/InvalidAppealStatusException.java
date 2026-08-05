package com.academy.trafficviolationsystem.core.exceptions.appeal;

import com.academy.trafficviolationsystem.core.exceptions.AppException;
import com.academy.trafficviolationsystem.core.exceptions.ErrorCode;
import org.springframework.http.HttpStatus;

public class InvalidAppealStatusException extends AppException {
    public InvalidAppealStatusException(String message) {
        super(HttpStatus.CONFLICT, ErrorCode.INVALID_APPEAL_STATUS, message);
    }
}