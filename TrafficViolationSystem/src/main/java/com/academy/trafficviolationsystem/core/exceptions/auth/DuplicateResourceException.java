package com.academy.trafficviolationsystem.core.exceptions.auth;

import com.academy.trafficviolationsystem.core.exceptions.AppException;
import com.academy.trafficviolationsystem.core.exceptions.ErrorCode;
import org.springframework.http.HttpStatus;

public class DuplicateResourceException extends AppException {
    public DuplicateResourceException(String message) {
        super(HttpStatus.CONFLICT, ErrorCode.DUPLICATE_RESOURCE, message);
    }
}
