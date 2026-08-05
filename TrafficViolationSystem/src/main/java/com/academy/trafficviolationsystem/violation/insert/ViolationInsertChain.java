package com.academy.trafficviolationsystem.violation.insert;

import com.academy.trafficviolationsystem.violation.ViolationCreateRequest;
import com.academy.trafficviolationsystem.violation.ViolationEntity;
import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Component;

/**
 * Wires the individual handlers into the fixed order beforeInsert() used
 * to run inline, and exposes a single handle() entry point for
 * ViolationService to call.
 *
 * Adding a new check later (duplicate detection, camera status, ...) means
 * writing one new ViolationInsertHandler and inserting it into the link()
 * sequence below — the existing handlers don't change.
 */
@Component
public class ViolationInsertChain {

    private final ReferenceNumberHandler referenceNumberHandler;
    private final VehicleValidationHandler vehicleValidationHandler;
    private final DriverAssignmentHandler driverAssignmentHandler;
    private final SpeedingValidationHandler speedingValidationHandler;
    private final StatusInitializationHandler statusInitializationHandler;

    public ViolationInsertChain(ReferenceNumberHandler referenceNumberHandler,
                                 VehicleValidationHandler vehicleValidationHandler,
                                 DriverAssignmentHandler driverAssignmentHandler,
                                 SpeedingValidationHandler speedingValidationHandler,
                                 StatusInitializationHandler statusInitializationHandler) {
        this.referenceNumberHandler = referenceNumberHandler;
        this.vehicleValidationHandler = vehicleValidationHandler;
        this.driverAssignmentHandler = driverAssignmentHandler;
        this.speedingValidationHandler = speedingValidationHandler;
        this.statusInitializationHandler = statusInitializationHandler;
    }

    @PostConstruct
    void link() {
        referenceNumberHandler
            .linkWith(vehicleValidationHandler)
            .linkWith(driverAssignmentHandler)
            .linkWith(speedingValidationHandler)
            .linkWith(statusInitializationHandler);
    }

    public void handle(ViolationCreateRequest request, ViolationEntity entity) {
        referenceNumberHandler.handle(request, entity);
    }
}
