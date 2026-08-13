package com.selfcare.platform.common.web;

import jakarta.servlet.http.HttpServletRequest;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.context.MessageSourceResolvable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

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
    private static final String TRACE_ID = "traceId";

    @ExceptionHandler(ApiException.class)
    public ResponseEntity<ApiResponse<Void>> handleApiException(ApiException ex, HttpServletRequest request) {
        String traceId = MDC.get(TRACE_ID);
        log.warn("Handled API exception on {} {}: {} ({})", request.getMethod(), request.getRequestURI(),
                ex.getMessage(), ex.getErrorCode());
        return ResponseEntity.status(ex.getStatus())
                .body(ApiResponse.failure(new ApiResponse.ApiError(ex.getErrorCode(), ex.getMessage(), traceId)));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Void>> handleValidation(MethodArgumentNotValidException ex) {
        String traceId = MDC.get(TRACE_ID);
        String message = ex.getBindingResult().getFieldErrors().stream()
                .map(FieldError::getDefaultMessage)
                .reduce((a, b) -> a + "; " + b)
                .orElse("Validation failed");
        return ResponseEntity.badRequest()
                .body(ApiResponse.failure(new ApiResponse.ApiError("VALIDATION_ERROR", message, traceId)));
    }

    /**
     * Spring's request-binding failures: a missing required parameter, a parameter that can't be
     * converted (an unknown enum constant, a malformed date), a body Jackson can't read, or a
     * constraint on a handler-method parameter. All of these are malformed CLIENT requests, so
     * they must not reach the catch-all below -- reporting a caller's mistake as a 500 blames the
     * service for it, and inflates the error rate that on-call alerting watches.
     */
    @ExceptionHandler({
        MissingServletRequestParameterException.class,
        MethodArgumentTypeMismatchException.class,
        HttpMessageNotReadableException.class,
        HandlerMethodValidationException.class
    })
    public ResponseEntity<ApiResponse<Void>> handleBadRequest(Exception ex, HttpServletRequest request) {
        String traceId = MDC.get(TRACE_ID);
        log.warn("Rejected malformed request on {} {}: {}", request.getMethod(), request.getRequestURI(),
                ex.getMessage());
        return ResponseEntity.badRequest()
                .body(ApiResponse.failure(new ApiResponse.ApiError("VALIDATION_ERROR", describe(ex), traceId)));
    }

    private static String describe(Exception ex) {
        if (ex instanceof MissingServletRequestParameterException missing) {
            return missing.getParameterName() + " is required";
        }
        if (ex instanceof MethodArgumentTypeMismatchException mismatch) {
            return mismatch.getName() + " has an invalid value";
        }
        if (ex instanceof HandlerMethodValidationException validation) {
            return validation.getAllErrors().stream()
                    .map(MessageSourceResolvable::getDefaultMessage)
                    .filter(Objects::nonNull)
                    .reduce((a, b) -> a + "; " + b)
                    .orElse("Validation failed");
        }
        // Jackson names the offending field but also leaks type/class internals, so the detail
        // stays on the log line above and the client gets the safe summary.
        return "Request body is malformed or contains an invalid value";
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleUnexpected(Exception ex) {
        String traceId = MDC.get(TRACE_ID);
        // Deliberately generic message to the client; the full exception is on the log line
        // that carries the same traceId, which is what Sentry/Kibana correlate on.
        log.error("Unhandled exception, traceId={}", traceId, ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.failure(new ApiResponse.ApiError("INTERNAL_ERROR", "An unexpected error occurred", traceId)));
    }
}
