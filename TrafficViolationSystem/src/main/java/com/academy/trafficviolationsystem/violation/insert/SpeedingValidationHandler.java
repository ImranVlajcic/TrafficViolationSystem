package com.academy.trafficviolationsystem.violation.insert;

import com.academy.trafficviolationsystem.core.exceptions.BadRequestException;
import com.academy.trafficviolationsystem.violation.ViolationCreateRequest;
import com.academy.trafficviolationsystem.violation.ViolationEntity;
import com.academy.trafficviolationsystem.violation.ViolationType;
import org.springframework.stereotype.Component;

/**
 * Validates that measuredSpeed/speedLimit are present, and that
 * measuredSpeed exceeds speedLimit, but only for SPEEDING violations.
 * No repository dependency — easiest handler to unit test in isolation
 * from the others.
 */
@Component
public class SpeedingValidationHandler extends ViolationInsertHandler {

    @Override
    protected void doHandle(ViolationCreateRequest request, ViolationEntity entity) {
        if (request.getViolationType() != ViolationType.SPEEDING) {
            return;
        }
        if (request.getMeasuredSpeed() == null || request.getSpeedLimit() == null) {
            throw new BadRequestException("measuredSpeed and speedLimit are required for SPEEDING violations");
        }
        if (request.getMeasuredSpeed() <= request.getSpeedLimit()) {
            throw new BadRequestException(
                "measuredSpeed (" + request.getMeasuredSpeed() + ") must exceed speedLimit (" +
                request.getSpeedLimit() + ") for a SPEEDING violation");
        }
    }
}
