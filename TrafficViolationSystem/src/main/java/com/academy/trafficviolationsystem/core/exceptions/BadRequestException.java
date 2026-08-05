package com.academy.trafficviolationsystem.core.exceptions;

import org.springframework.http.HttpStatus;

/**
 * Thrown when input is structurally valid but violates a business rule.
 * Produces HTTP 400 Bad Request.
 *
 * Use this for business-rule failures that @Valid cannot catch:
 *
 * For format/annotation failures (@NotNull, @Size, etc.) GlobalExceptionHandler
 * handles MethodArgumentNotValidException automatically — no need to throw here.
 */
public class BadRequestException extends AppException {

    public BadRequestException(String message) {
        super(HttpStatus.BAD_REQUEST, ErrorCode.BAD_REQUEST, message);
    }
}
