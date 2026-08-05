package com.academy.trafficviolationsystem.core.exceptions.vehicle;

import com.academy.trafficviolationsystem.core.exceptions.AppException;
import com.academy.trafficviolationsystem.core.exceptions.ErrorCode;
import org.springframework.http.HttpStatus;

public class VehicleAlreadyStolenException extends AppException {
    public VehicleAlreadyStolenException(Object vehicleId) {
        super(HttpStatus.CONFLICT, ErrorCode.VEHICLE_ALREADY_STOLEN,
                "Vehicle " + vehicleId + " is already marked as stolen");
    }
}
