package com.selfcare.loyalty.service;

import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.stereotype.Component;

/**
 * Local-dev-only fallback so this service can run standalone (docker-compose, no real Auth
 * Service available) without every call failing. NEVER select this in a real environment --
 * set {@code auth.service.base-url} so {@link AuthServiceCustomerValidationClient} is used
 * instead; this bean logs a loud warning on startup precisely so that mistake is visible.
 */
@Component
@ConditionalOnMissingBean(AuthServiceCustomerValidationClient.class)
public class NoOpCustomerValidationClient implements CustomerValidationClient {

    private static final Logger log = LoggerFactory.getLogger(NoOpCustomerValidationClient.class);

    @PostConstruct
    void warnDevMode() {
        log.warn("*** auth.service.base-url is not set -- using NoOpCustomerValidationClient. "
                + "Every customer validation will succeed. This must NEVER run in a real environment. ***");
    }

    @Override
    public boolean isValidCustomer(String nationalId, String subscriberMsisdn) {
        return true;
    }
}
