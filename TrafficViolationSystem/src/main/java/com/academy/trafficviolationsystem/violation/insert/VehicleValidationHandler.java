package com.academy.trafficviolationsystem.violation.insert;

import com.academy.trafficviolationsystem.core.exceptions.BadRequestException;
import com.academy.trafficviolationsystem.core.exceptions.NotFoundException;
import com.academy.trafficviolationsystem.vehicle.VehicleEntity;
import com.academy.trafficviolationsystem.vehicle.VehicleRepository;
import com.academy.trafficviolationsystem.violation.ViolationCreateRequest;
import com.academy.trafficviolationsystem.violation.ViolationEntity;
import org.springframework.stereotype.Component;

/**
 * Loads the required vehicle and rejects violations recorded against a
 * deregistered vehicle. Same exception types and messages as the original
 * inline check in beforeInsert().
 */
@Component
public class VehicleValidationHandler extends ViolationInsertHandler {

    private final VehicleRepository vehicleRepository;

    public VehicleValidationHandler(VehicleRepository vehicleRepository) {
        this.vehicleRepository = vehicleRepository;
    }

    @Override
    protected void doHandle(ViolationCreateRequest request, ViolationEntity entity) {
        VehicleEntity vehicle = vehicleRepository.findById(request.getVehicleId())
                .orElseThrow(() -> new NotFoundException("Vehicle " + request.getVehicleId() + " not found"));
        if (!vehicle.isActive()) {
            throw new BadRequestException("Cannot record a violation for a deregistered vehicle");
        }
        entity.setVehicle(vehicle);
    }
}
