package com.academy.trafficviolationsystem.core.exceptions.auth;

import com.academy.trafficviolationsystem.core.exceptions.AppException;
import com.academy.trafficviolationsystem.core.exceptions.ErrorCode;
import org.springframework.http.HttpStatus;

public class TokenExpiredException extends AppException {
    public TokenExpiredException() {
        super(HttpStatus.UNAUTHORIZED, ErrorCode.TOKEN_EXPIRED, "JWT token has expired");
    }
}
