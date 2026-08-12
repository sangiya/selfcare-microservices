package com.selfcare.loyalty.service;

/**
 * Replaces {@code SCAppUtils::validateCustomer()} from the legacy controller: confirms the
 * caller's national ID matches the authenticated subscriber MSISDN before any loyalty action
 * proceeds. Backed by the existing Auth Service (Doc 1 sec 2.1) -- this service does not own
 * identity data.
 */
public interface CustomerValidationClient {

    boolean isValidCustomer(String nationalId, String subscriberMsisdn);
}
