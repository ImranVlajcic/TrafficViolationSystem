package com.academy.trafficviolationsystem.core.exceptions.auth;

import com.academy.trafficviolationsystem.core.exceptions.AppException;
import com.academy.trafficviolationsystem.core.exceptions.ErrorCode;
import org.springframework.http.HttpStatus;

public class AccountLockedException extends AppException {
    public AccountLockedException() {
        this("Account is temporarily locked due to too many failed login attempts");
    }
    public AccountLockedException(String message) {
        super(HttpStatus.LOCKED, ErrorCode.ACCOUNT_LOCKED, message);
    }
}
