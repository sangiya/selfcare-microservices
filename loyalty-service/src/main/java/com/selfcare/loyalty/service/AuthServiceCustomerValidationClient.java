package com.selfcare.loyalty.service;

import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

/**
 * Calls the existing Auth Service's customer-validation endpoint. Enabled whenever
 * {@code auth.service.base-url} is set; see {@link NoOpCustomerValidationClient} for the local
 * dev fallback when it isn't.
 */
@Component
@org.springframework.boot.autoconfigure.condition.ConditionalOnExpression(
        "T(org.springframework.util.StringUtils).hasText('${auth.service.base-url:}')")
public class AuthServiceCustomerValidationClient implements CustomerValidationClient {

    private static final Logger log = LoggerFactory.getLogger(AuthServiceCustomerValidationClient.class);

    private final RestClient restClient;

    public AuthServiceCustomerValidationClient(@Value("${auth.service.base-url}") String baseUrl) {
        this.restClient = RestClient.builder().baseUrl(baseUrl).build();
    }

    @Override
    @SuppressWarnings("unchecked")
    public boolean isValidCustomer(String nationalId, String subscriberMsisdn) {
        try {
            Map<String, Object> response = restClient.get()
                    .uri(uriBuilder -> uriBuilder.path("/api/v1/customers/validate")
                            .queryParam("nationalId", nationalId)
                            .queryParam("msisdn", subscriberMsisdn)
                            .build())
                    .retrieve()
                    .body(Map.class);
            return response != null && Boolean.TRUE.equals(response.get("valid"));
        } catch (RestClientException ex) {
            // Fail closed: an unreachable Auth Service must never be treated as "customer is
            // valid" -- that would silently disable authorization.
            log.error("Auth Service customer validation call failed, failing closed", ex);
            return false;
        }
    }
}
