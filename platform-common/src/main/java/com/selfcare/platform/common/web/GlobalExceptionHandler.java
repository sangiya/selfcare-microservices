package com.selfcare.platform.common.web;

import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Translates every exception into the standard {@link ApiResponse} envelope and logs it once,
 * with the correlation ID (see {@code CorrelationIdFilter}) attached — this is what lets a
 * Sentry error be traced back to the exact request/release/operator (Doc 2 sec 4). Every
 * service inherits this automatically via platform-common's auto-configuration; a service only
 * needs its own {@code @RestControllerAdvice} for exceptions that need bespoke handling beyond
 * the standard envelope.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(ApiException.class)
    public ResponseEntity<ApiResponse<Void>> handleApiException(ApiException ex, HttpServletRequest request) {
        String traceId = MDC.get("traceId");
        log.warn("Handled API exception on {} {}: {} ({})", request.getMethod(), request.getRequestURI(),
                ex.getMessage(), ex.getErrorCode());
        return ResponseEntity.status(ex.getStatus())
                .body(ApiResponse.failure(new ApiResponse.ApiError(ex.getErrorCode(), ex.getMessage(), traceId)));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Void>> handleValidation(MethodArgumentNotValidException ex) {
        String traceId = MDC.get("traceId");
        String message = ex.getBindingResult().getFieldErrors().stream()
                .map(FieldError::getDefaultMessage)
                .reduce((a, b) -> a + "; " + b)
                .orElse("Validation failed");
        return ResponseEntity.badRequest()
                .body(ApiResponse.failure(new ApiResponse.ApiError("VALIDATION_ERROR", message, traceId)));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleUnexpected(Exception ex) {
        String traceId = MDC.get("traceId");
        // Deliberately generic message to the client; the full exception is on the log line
        // that carries the same traceId, which is what Sentry/Kibana correlate on.
        log.error("Unhandled exception, traceId={}", traceId, ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.failure(new ApiResponse.ApiError("INTERNAL_ERROR", "An unexpected error occurred", traceId)));
    }
}
