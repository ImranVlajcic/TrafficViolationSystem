package com.academy.trafficviolationsystem.core.exceptions.driver;

import com.academy.trafficviolationsystem.core.exceptions.AppException;
import com.academy.trafficviolationsystem.core.exceptions.ErrorCode;
import org.springframework.http.HttpStatus;

public class DriverNotSuspendedException extends AppException {
    public DriverNotSuspendedException(Object driverId) {
        super(HttpStatus.CONFLICT, ErrorCode.DRIVER_NOT_SUSPENDED,
                "Driver " + driverId + " is not currently suspended");
    }
}
