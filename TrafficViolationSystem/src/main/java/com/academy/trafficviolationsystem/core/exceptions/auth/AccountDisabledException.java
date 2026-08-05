package com.academy.trafficviolationsystem.core.exceptions.auth;

import com.academy.trafficviolationsystem.core.exceptions.AppException;
import com.academy.trafficviolationsystem.core.exceptions.ErrorCode;
import org.springframework.http.HttpStatus;

public class AccountDisabledException extends AppException {
    public AccountDisabledException() {
        super(HttpStatus.FORBIDDEN, ErrorCode.ACCOUNT_DISABLED, "Account has been disabled");
    }
}
