package com.academy.trafficviolationsystem.core.exceptions.fine;

import com.academy.trafficviolationsystem.core.exceptions.AppException;
import com.academy.trafficviolationsystem.core.exceptions.ErrorCode;
import com.academy.trafficviolationsystem.fine.FineStatus;
import org.springframework.http.HttpStatus;

public class FineNotDisputableException extends AppException {
    public FineNotDisputableException(Object id, FineStatus currentStatus) {
        super(HttpStatus.CONFLICT, ErrorCode.FINE_NOT_DISPUTABLE,
                "Fine " + id + " cannot be disputed in status: " + currentStatus);
    }
}
