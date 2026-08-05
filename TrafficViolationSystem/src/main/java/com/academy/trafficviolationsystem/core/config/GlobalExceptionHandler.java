package com.academy.trafficviolationsystem.core.config;

import com.academy.trafficviolationsystem.core.exceptions.AppException;
import com.academy.trafficviolationsystem.core.exceptions.ErrorCode;
import com.academy.trafficviolationsystem.core.model.ApiResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.util.Map;
import java.util.stream.Collectors;

/**
 * Central error handling for the entire application.
 *
 * Every handler returns the same ErrorResponse shape so the frontend
 * can rely on a consistent contract : { data, error, message }
 *
 * Handler priority (most specific wins):
 *   AppException subtypes  → uses the status/code baked into the exception
 *   Spring Security        → 401 / 403
 *   Validation             → 400 with field-level details in 'data'
 *   Generic Exception      → 500 (never leaks stack traces to client)
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(AppException.class)
    public ResponseEntity<ApiResponse<Object>> handleAppException(AppException ex) {
        return new ResponseEntity<>(
                ApiResponse.fail(ex.getError(), ex.getMessage()),
                ex.getStatus()
        );
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiResponse<Object>> handleAccessDenied(AccessDeniedException ex) {
        return new ResponseEntity<>(
                ApiResponse.fail(ErrorCode.FORBIDDEN, "You do not have permission to perform this action"),
                HttpStatus.FORBIDDEN
        );
    }

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ApiResponse<Object>> handleAuthentication(AuthenticationException ex) {
        return new ResponseEntity<>(
                ApiResponse.fail(ErrorCode.UNAUTHORIZED, "Authentication is required to access this resource"),
                HttpStatus.UNAUTHORIZED
        );
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Object>> handleValidationException(MethodArgumentNotValidException ex) {
        Map<String, String> fieldErrors = ex.getBindingResult().getFieldErrors().stream()
                .collect(Collectors.toMap(
                        fe -> fe.getField(),
                        fe -> fe.getDefaultMessage() != null ? fe.getDefaultMessage() : "Invalid value",
                        (first, second) -> first
                ));
        return new ResponseEntity<>(
                ApiResponse.fail(ErrorCode.VALIDATION_FAILED, "Request validation failed", fieldErrors),
                HttpStatus.BAD_REQUEST
        );
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ApiResponse<Object>> handleMissingParam(MissingServletRequestParameterException ex) {
        return new ResponseEntity<>(
                ApiResponse.fail(ErrorCode.BAD_REQUEST, "Required parameter '" + ex.getParameterName() + "' is missing"),
                HttpStatus.BAD_REQUEST
        );
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ApiResponse<Object>> handleTypeMismatch(MethodArgumentTypeMismatchException ex) {
        String detail = String.format(
                "Parameter '%s' must be of type %s",
                ex.getName(),
                ex.getRequiredType() != null ? ex.getRequiredType().getSimpleName() : "unknown"
        );
        return new ResponseEntity<>(
                ApiResponse.fail(ErrorCode.BAD_REQUEST, detail),
                HttpStatus.BAD_REQUEST
        );
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Object>> handleGenericException(Exception ex) {
        ex.printStackTrace();
        return new ResponseEntity<>(
                ApiResponse.fail(ErrorCode.UNKNOWN_ERROR, "An unexpected error occurred. Please try again later."),
                HttpStatus.INTERNAL_SERVER_ERROR
        );
    }
}
