package com.academy.trafficviolationsystem.core.exceptions.appeal;

import com.academy.trafficviolationsystem.core.exceptions.AppException;
import com.academy.trafficviolationsystem.core.exceptions.ErrorCode;
import org.springframework.http.HttpStatus;

public class AppealAlreadyExistsException extends AppException {
    public AppealAlreadyExistsException(String appealNumber, String violationReference) {
        super(HttpStatus.CONFLICT, ErrorCode.APPEAL_ALREADY_EXISTS,
                "An active appeal (" + appealNumber + ") already exists for violation " + violationReference);
    }
}
