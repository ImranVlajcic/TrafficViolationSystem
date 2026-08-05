package com.academy.trafficviolationsystem.core.exceptions.appeal;

import com.academy.trafficviolationsystem.core.exceptions.AppException;
import com.academy.trafficviolationsystem.core.exceptions.ErrorCode;
import org.springframework.http.HttpStatus;

public class AppealAccessDeniedException extends AppException {
    public AppealAccessDeniedException(String message) {
        super(HttpStatus.FORBIDDEN, ErrorCode.FORBIDDEN, message);
    }
}