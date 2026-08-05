package com.academy.trafficviolationsystem.core.exceptions.driver;

import com.academy.trafficviolationsystem.core.exceptions.AppException;
import com.academy.trafficviolationsystem.core.exceptions.ErrorCode;
import org.springframework.http.HttpStatus;

public class DriverAlreadySuspendedException extends AppException {
    public DriverAlreadySuspendedException(Object driverId) {
        super(HttpStatus.CONFLICT, ErrorCode.DRIVER_ALREADY_SUSPENDED,
                "Driver " + driverId + " is already suspended — lift the current suspension first");
    }
}
