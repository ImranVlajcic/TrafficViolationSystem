package com.academy.trafficviolationsystem.core.exceptions.vehicle;

import com.academy.trafficviolationsystem.core.exceptions.AppException;
import com.academy.trafficviolationsystem.core.exceptions.ErrorCode;
import org.springframework.http.HttpStatus;

public class VehicleNotStolenException extends AppException {
    public VehicleNotStolenException(Object vehicleId) {
        super(HttpStatus.CONFLICT, ErrorCode.VEHICLE_NOT_STOLEN,
                "Vehicle " + vehicleId + " is not marked as stolen");
    }
}
