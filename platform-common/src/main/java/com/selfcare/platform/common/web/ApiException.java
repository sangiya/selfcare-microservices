package com.selfcare.platform.common.web;

import org.springframework.http.HttpStatus;

/**
 * Base class for exceptions meant to be translated into a client-facing {@link ApiResponse}
 * by {@link GlobalExceptionHandler}. Prefer a specific subclass ({@link NotFoundException},
 * {@link BadRequestException}) or a domain-specific one per service (e.g.
 * {@code InsufficientPointsException} in loyalty-service) over throwing this directly.
 */
public class ApiException extends RuntimeException {

    private final String errorCode;
    private final HttpStatus status;

    public ApiException(String errorCode, String message, HttpStatus status) {
        super(message);
        this.errorCode = errorCode;
        this.status = status;
    }

    public String getErrorCode() {
        return errorCode;
    }

    public HttpStatus getStatus() {
        return status;
    }
}
