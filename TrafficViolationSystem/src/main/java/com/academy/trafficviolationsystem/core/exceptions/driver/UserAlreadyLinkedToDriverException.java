package com.academy.trafficviolationsystem.core.exceptions.driver;

import com.academy.trafficviolationsystem.core.exceptions.AppException;
import com.academy.trafficviolationsystem.core.exceptions.ErrorCode;
import org.springframework.http.HttpStatus;

public class UserAlreadyLinkedToDriverException extends AppException {
    public UserAlreadyLinkedToDriverException(Object userId) {
        super(HttpStatus.CONFLICT, ErrorCode.USER_ALREADY_LINKED,
                "User " + userId + " is already linked to a driver record");
    }
}
