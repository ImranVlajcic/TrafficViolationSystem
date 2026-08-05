package com.academy.trafficviolationsystem.core.exceptions.auth;

import com.academy.trafficviolationsystem.core.exceptions.AppException;
import com.academy.trafficviolationsystem.core.exceptions.ErrorCode;
import org.springframework.http.HttpStatus;

// ─────────────────────────────────────────────────────────────────────────────
// Auth exceptions
// ─────────────────────────────────────────────────────────────────────────────

public class UnauthorizedException extends AppException {
    public UnauthorizedException(String message) {
        super(HttpStatus.UNAUTHORIZED, ErrorCode.UNAUTHORIZED, message);
    }
}
