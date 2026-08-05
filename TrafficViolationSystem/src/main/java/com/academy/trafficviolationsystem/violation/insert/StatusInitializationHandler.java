package com.academy.trafficviolationsystem.violation.insert;

import com.academy.trafficviolationsystem.violation.DetectionMethod;
import com.academy.trafficviolationsystem.violation.ViolationCreateRequest;
import com.academy.trafficviolationsystem.violation.ViolationEntity;
import com.academy.trafficviolationsystem.violation.ViolationStatus;
import org.springframework.stereotype.Component;

/**
 * Last link in the chain: derives isAutomatic from the detection method
 * and sets the initial status (PENDING for an automatic detection,
 * CONFIRMED for a manual officer entry). Placed last to mirror the
 * original method's ordering — nothing downstream depends on it running
 * before the other handlers.
 */
@Component
public class StatusInitializationHandler extends ViolationInsertHandler {

    @Override
    protected void doHandle(ViolationCreateRequest request, ViolationEntity entity) {
        boolean isAuto = request.getDetectionMethod() != DetectionMethod.MANUAL_OFFICER;
        entity.setAutomatic(isAuto);
        entity.setStatus(isAuto ? ViolationStatus.PENDING : ViolationStatus.CONFIRMED);
    }
}
