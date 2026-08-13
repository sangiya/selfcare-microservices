package com.selfcare.qa.api;

import java.net.URI;

/**
 * Every base URL is overridable via env var so this suite can point at docker-compose on
 * localhost (defaults below), a Jenkins-deployed dev namespace, or anywhere else -- see
 * README.md for the full list and how the Jenkinsfile's QA Automation stage overrides them.
 */
public final class ApiTestConfig {

    public static final String GATEWAY_URL = env("GATEWAY_URL", "http://localhost:8080");
    public static final String LOYALTY_URL = envOrDerived("LOYALTY_URL", GATEWAY_URL, 8082, "http://localhost:8082");
    public static final String REPORTS_URL = envOrDerived("REPORTS_URL", GATEWAY_URL, 8083, "http://localhost:8083");
    public static final String NOTIFICATION_URL = envOrDerived("NOTIFICATION_URL", GATEWAY_URL, 8084, "http://localhost:8084");
    public static final String CONTENT_URL = envOrDerived("CONTENT_URL", GATEWAY_URL, 8085, "http://localhost:8085");
    public static final String CONFIG_TENANT_URL = envOrDerived("CONFIG_TENANT_URL", GATEWAY_URL, 8081, "http://localhost:8081");

    public static final String TENANT_HEADER = "X-Tenant-Id";
    public static final String TEST_TENANT_ID = env("TEST_TENANT_ID", "acme-telecom");

    private ApiTestConfig() {
    }

    private static String env(String name, String fallback) {
        String value = System.getenv(name);
        return (value == null || value.isBlank()) ? fallback : value;
    }

    private static String envOrDerived(String name, String gatewayUrl, int port, String fallback) {
        String value = System.getenv(name);
        if (value != null && !value.isBlank()) {
            return value;
        }

        try {
            URI gatewayUri = URI.create(gatewayUrl);
            if (gatewayUri.getScheme() != null && gatewayUri.getHost() != null) {
                return new URI(
                        gatewayUri.getScheme(),
                        gatewayUri.getUserInfo(),
                        gatewayUri.getHost(),
                        port,
                        null,
                        null,
                        null
                ).toString();
            }
        } catch (IllegalArgumentException ignored) {
            // Fall through to the hardcoded localhost default when the gateway URL is malformed.
        } catch (Exception ignored) {
            // URI construction can also throw checked syntax exceptions; use the fallback then.
        }

        return fallback;
    }
}
