package com.academy.trafficviolationsystem.core.exceptions.appeal;

import com.academy.trafficviolationsystem.core.exceptions.AppException;
import com.academy.trafficviolationsystem.core.exceptions.ErrorCode;
import org.springframework.http.HttpStatus;

public class AppealWindowClosedException extends AppException {
    public AppealWindowClosedException(String violationReference, long daysLate, int windowDays) {
        super(HttpStatus.GONE, ErrorCode.APPEAL_WINDOW_CLOSED,
                "The appeal window for violation " + violationReference + " closed " + daysLate +
                        " days ago. Appeals must be submitted within " + windowDays + " days of the violation.");
    }
}