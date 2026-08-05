package com.academy.trafficviolationsystem.core.exceptions;

import lombok.Getter;
import org.springframework.http.HttpStatus;

/**
 * Base exception for all intentional application errors.
 *
 * Every domain exception (NotFoundException, FineAlreadyPaidException, etc.)
 * extends this class. GlobalExceptionHandler catches AppException and converts
 * it to an HTTP response using the status and error code stored here, so you
 * never need to write error-handling logic in controllers.
 *
 */
@Getter
public class AppException extends RuntimeException {

    private final HttpStatus status;
    private final ErrorCode error;

    public AppException(HttpStatus status, ErrorCode error, String message) {
        super(message);
        this.status = status;
        this.error = error;
    }
}
