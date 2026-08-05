package com.academy.trafficviolationsystem.violation.insert;

import com.academy.trafficviolationsystem.core.exceptions.NotFoundException;
import com.academy.trafficviolationsystem.driver.DriverEntity;
import com.academy.trafficviolationsystem.driver.DriverRepository;
import com.academy.trafficviolationsystem.violation.ViolationCreateRequest;
import com.academy.trafficviolationsystem.violation.ViolationEntity;
import org.springframework.stereotype.Component;

/** Loads the optional driver, if one was supplied on the create request. */
@Component
public class DriverAssignmentHandler extends ViolationInsertHandler {

    private final DriverRepository driverRepository;

    public DriverAssignmentHandler(DriverRepository driverRepository) {
        this.driverRepository = driverRepository;
    }

    @Override
    protected void doHandle(ViolationCreateRequest request, ViolationEntity entity) {
        if (request.getDriverId() != null) {
            DriverEntity driver = driverRepository.findById(request.getDriverId())
                    .orElseThrow(() -> new NotFoundException("Driver " + request.getDriverId() + " not found"));
            entity.setDriver(driver);
        }
    }
}
