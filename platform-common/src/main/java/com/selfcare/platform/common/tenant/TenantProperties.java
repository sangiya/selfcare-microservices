package com.selfcare.platform.common.tenant;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Bound from {@code platform.tenant.*} in each service's application.yml, and in turn from
 * that service's per-operator Helm values overlay (see deploy/helm/values/operator-values).
 */
@ConfigurationProperties(prefix = "platform.tenant")
public class TenantProperties {

    /**
     * The operator this deployment serves. Set via the TENANT_ID env var / Helm values for a
     * single-operator deployment (the normal case per Doc 1 sec 5). Used as the fallback when
     * a request carries no explicit tenant header.
     */
    private String defaultTenantId = "default";

    /**
     * Header used to resolve tenant per-request when a deployment genuinely serves more than
     * one operator (e.g. a shared edge/gateway tier).
     */
    private String headerName = "X-Tenant-Id";

    public String getDefaultTenantId() {
        return defaultTenantId;
    }

    public void setDefaultTenantId(String defaultTenantId) {
        this.defaultTenantId = defaultTenantId;
    }

    public String getHeaderName() {
        return headerName;
    }

    public void setHeaderName(String headerName) {
        this.headerName = headerName;
    }
}
