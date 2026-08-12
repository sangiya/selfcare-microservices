package com.selfcare.platform.common.tenant;

/**
 * Holds the resolved operator/tenant identity for the current request thread.
 *
 * <p>Design note (see Doc 1 sec 5 and sec 6.1): each operator is normally deployed into its
 * own cluster/VPN, so most of the time there is exactly one tenant per running instance,
 * configured via the {@code TENANT_ID} environment variable in that operator's Helm values
 * overlay. The same code path also supports a request-scoped override (header/host based),
 * so a single shared-gateway deployment can still resolve tenant per request when a service
 * genuinely does front more than one operator. Every downstream call — Config Service lookups,
 * API Adapter Registry resolution, DB/cache key prefixing — reads from here rather than each
 * layer re-implementing tenant detection.
 */
public final class TenantContext {

    private static final ThreadLocal<String> CURRENT_TENANT = new ThreadLocal<>();

    private TenantContext() {
    }

    public static void set(String tenantId) {
        CURRENT_TENANT.set(tenantId);
    }

    public static String get() {
        String tenantId = CURRENT_TENANT.get();
        if (tenantId == null || tenantId.isBlank()) {
            throw new IllegalStateException(
                    "No tenant resolved for this request. Ensure TenantResolverFilter ran, "
                            + "or that the 'platform.tenant.default-tenant-id' property is set for "
                            + "single-operator deployments.");
        }
        return tenantId;
    }

    public static String getOrDefault(String fallback) {
        String tenantId = CURRENT_TENANT.get();
        return (tenantId == null || tenantId.isBlank()) ? fallback : tenantId;
    }

    public static void clear() {
        CURRENT_TENANT.remove();
    }
}
