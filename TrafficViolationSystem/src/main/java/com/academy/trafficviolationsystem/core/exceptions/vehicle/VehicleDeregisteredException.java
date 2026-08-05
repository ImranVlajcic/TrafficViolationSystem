package com.academy.trafficviolationsystem.core.exceptions.vehicle;

import com.academy.trafficviolationsystem.core.exceptions.AppException;
import com.academy.trafficviolationsystem.core.exceptions.ErrorCode;
import org.springframework.http.HttpStatus;

public class VehicleDeregisteredException extends AppException {
    public VehicleDeregisteredException(Object vehicleId) {
        super(HttpStatus.CONFLICT, ErrorCode.VEHICLE_DEREGISTERED,
                "Cannot transfer ownership of deregistered vehicle " + vehicleId);
    }
}
