package com.academy.trafficviolationsystem.core.model;

import com.academy.trafficviolationsystem.core.exceptions.ErrorCode;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Standard wrapper for ALL API responses — success and error alike.
 *
 * Shape is always: { success, message, error, data }
 *   - on success: error is null
 *   - on failure: data is null
 *
 * Example success JSON:
 * { "success": true, "message": "Fine paid successfully", "error": null, "data": { ... } }
 *
 * Example error JSON:
 * { "success": false, "message": "Fine not found", "error": "NOT_FOUND", "data": null }
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL) // keeps payloads lean; null error/data just omitted
public class ApiResponse<T> {

    private boolean success;
    private String message;
    private ErrorCode error;
    private T data;

    // ── success helpers ────────────────────────────────────────────────────

    public static <T> ApiResponse<T> ok(T data) {
        return new ApiResponse<>(true, "Success", null, data);
    }

    public static <T> ApiResponse<T> ok(String message, T data) {
        return new ApiResponse<>(true, message, null, data);
    }

    public static <T> ApiResponse<T> created(T data) {
        return new ApiResponse<>(true, "Created successfully", null, data);
    }

    public static <T> ApiResponse<T> created(String message, T data) {
        return new ApiResponse<>(true, message, null, data);
    }

    // ── failure helpers ────────────────────────────────────────────────────

    public static <T> ApiResponse<T> fail(ErrorCode error, String message) {
        return new ApiResponse<>(false, message, error, null);
    }

    /** Use when the error needs to carry data, e.g. field-level validation errors. */
    public static <T> ApiResponse<T> fail(ErrorCode error, String message, T data) {
        return new ApiResponse<>(false, message, error, data);
    }
}