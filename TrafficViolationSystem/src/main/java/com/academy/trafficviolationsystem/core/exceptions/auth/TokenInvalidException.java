package com.academy.trafficviolationsystem.core.exceptions.auth;

import com.academy.trafficviolationsystem.core.exceptions.AppException;
import com.academy.trafficviolationsystem.core.exceptions.ErrorCode;
import org.springframework.http.HttpStatus;

public class TokenInvalidException extends AppException {
    public TokenInvalidException() {
        this("JWT token is invalid");
    }
    public TokenInvalidException(String message) {
        super(HttpStatus.UNAUTHORIZED, ErrorCode.TOKEN_INVALID, message);
    }
}
