package com.selfcare.qa.api;

/**
 * Every base URL is overridable via env var so this suite can point at docker-compose on
 * localhost (defaults below), a Jenkins-deployed dev namespace, or anywhere else -- see
 * README.md for the full list and how the Jenkinsfile's QA Automation stage overrides them.
 */
public final class ApiTestConfig {

    public static final String GATEWAY_URL = env("GATEWAY_URL", "http://localhost:8080");
    public static final String LOYALTY_URL = env("LOYALTY_URL", "http://localhost:8082");
    public static final String REPORTS_URL = env("REPORTS_URL", "http://localhost:8083");
    public static final String NOTIFICATION_URL = env("NOTIFICATION_URL", "http://localhost:8084");
    public static final String CONTENT_URL = env("CONTENT_URL", "http://localhost:8085");
    public static final String CONFIG_TENANT_URL = env("CONFIG_TENANT_URL", "http://localhost:8081");

    public static final String TENANT_HEADER = "X-Tenant-Id";
    public static final String TEST_TENANT_ID = env("TEST_TENANT_ID", "acme-telecom");

    private ApiTestConfig() {
    }

    private static String env(String name, String fallback) {
        String value = System.getenv(name);
        return (value == null || value.isBlank()) ? fallback : value;
    }
}
