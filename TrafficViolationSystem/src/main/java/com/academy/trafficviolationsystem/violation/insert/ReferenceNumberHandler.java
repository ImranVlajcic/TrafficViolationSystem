package com.academy.trafficviolationsystem.violation.insert;

import com.academy.trafficviolationsystem.violation.ViolationCreateRequest;
import com.academy.trafficviolationsystem.violation.ViolationEntity;
import org.springframework.stereotype.Component;

/** First link in the chain: assigns the TRF-{YEAR}-{seq} reference number. */
@Component
public class ReferenceNumberHandler extends ViolationInsertHandler {

    private final ReferenceNumberGenerator referenceNumberGenerator;

    public ReferenceNumberHandler(ReferenceNumberGenerator referenceNumberGenerator) {
        this.referenceNumberGenerator = referenceNumberGenerator;
    }

    @Override
    protected void doHandle(ViolationCreateRequest request, ViolationEntity entity) {
        entity.setReferenceNumber(referenceNumberGenerator.generate());
    }
}
