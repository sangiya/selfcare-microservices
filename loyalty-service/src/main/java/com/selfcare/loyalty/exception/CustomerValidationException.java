package com.selfcare.loyalty.exception;

import com.selfcare.platform.common.web.ApiException;
import org.springframework.http.HttpStatus;

/** Equivalent to the legacy {@code auth_failed} branch in every StarPointsController action. */
public class CustomerValidationException extends ApiException {
    public CustomerValidationException(String message) {
        super("CUSTOMER_VALIDATION_FAILED", message, HttpStatus.UNAUTHORIZED);
    }
}
