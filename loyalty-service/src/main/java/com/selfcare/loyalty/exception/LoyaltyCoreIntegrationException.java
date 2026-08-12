package com.selfcare.loyalty.exception;

import com.selfcare.platform.common.web.ApiException;
import org.springframework.http.HttpStatus;

/**
 * Thrown when the operator's loyalty core system (MIFE Star Points in the audited codebase, or
 * whichever adapter is bound for this tenant) is unreachable or returns an unexpected result --
 * equivalent to the legacy {@code 'info' => 'error.system'} branches.
 */
public class LoyaltyCoreIntegrationException extends ApiException {
    public LoyaltyCoreIntegrationException(String message) {
        super("LOYALTY_CORE_UNAVAILABLE", message, HttpStatus.BAD_GATEWAY);
    }

    public LoyaltyCoreIntegrationException(String message, Throwable cause) {
        super("LOYALTY_CORE_UNAVAILABLE", message, HttpStatus.BAD_GATEWAY);
        initCause(cause);
    }
}
