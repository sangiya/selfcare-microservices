package com.selfcare.platform.common.web;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.time.Instant;

/**
 * Standard response envelope used by every service, replacing the ad hoc
 * {@code {'success' => true/false, 'info' => ...}} shape scattered across the legacy PHP
 * controllers with one consistent, typed contract.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ApiResponse<T>(boolean success, T data, ApiError error, Instant timestamp) {

    public static <T> ApiResponse<T> ok(T data) {
        return new ApiResponse<>(true, data, null, Instant.now());
    }

    public static <T> ApiResponse<T> failure(ApiError error) {
        return new ApiResponse<>(false, null, error, Instant.now());
    }

    public record ApiError(String code, String message, String traceId) {
    }
}
