package com.selfcare.platform.common.adapter;

import com.selfcare.platform.common.tenant.TenantContext;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Runtime lookup for {@link ApiAdapter} implementations, keyed by (capability interface,
 * tenant ID). Spring auto-wires every adapter bean of a given interface type into this
 * registry at startup (see {@link #afterPropertiesSet()} is not needed — registration happens
 * via constructor injection of {@code List<T>} in each capability's own configuration; this
 * class just holds the resolved map and answers lookups).
 *
 * <p>Usage in a service:
 * <pre>{@code
 * public interface LoyaltyCoreAdapter extends ApiAdapter {
 *     LoyaltyBalance getBalance(String subscriberId);
 * }
 *
 * @Component
 * class MifeLoyaltyCoreAdapter implements LoyaltyCoreAdapter {
 *     public String tenantId() { return ApiAdapter.ALL_TENANTS; } // default for every operator
 *     ...
 * }
 *
 * // in the business service:
 * LoyaltyCoreAdapter adapter = registry.resolve(LoyaltyCoreAdapter.class);
 * }</pre>
 *
 * <p>Registered as a bean by {@code PlatformAutoConfiguration} — no {@code @Component}
 * scanning across module boundaries required.
 */
public class ApiAdapterRegistry {

    private final Map<Class<? extends ApiAdapter>, Map<String, ApiAdapter>> byCapabilityAndTenant =
            new ConcurrentHashMap<>();

    /**
     * Registers a set of adapters implementing the same capability interface. Called once per
     * capability from that capability's {@code @Configuration} class (see loyalty-service's
     * {@code LoyaltyAdapterConfig} for a full example) so the registry stays generic here and
     * doesn't need to know about every capability interface in the platform.
     */
    public <T extends ApiAdapter> void register(Class<T> capability, List<T> adapters) {
        Map<String, ApiAdapter> byTenant = byCapabilityAndTenant.computeIfAbsent(capability, c -> new ConcurrentHashMap<>());
        for (T adapter : adapters) {
            byTenant.put(adapter.tenantId(), adapter);
        }
    }

    /**
     * Resolves the adapter for the current tenant (from {@link TenantContext}), falling back
     * to the {@link ApiAdapter#ALL_TENANTS} default adapter if no operator-specific override
     * is registered.
     */
    @SuppressWarnings("unchecked")
    public <T extends ApiAdapter> T resolve(Class<T> capability) {
        String tenantId = TenantContext.get();
        Map<String, ApiAdapter> byTenant = byCapabilityAndTenant.get(capability);
        if (byTenant == null || byTenant.isEmpty()) {
            throw new IllegalStateException("No adapters registered for capability " + capability.getSimpleName()
                    + ". Register at least a '" + ApiAdapter.ALL_TENANTS + "' default adapter at startup.");
        }
        ApiAdapter adapter = byTenant.get(tenantId);
        if (adapter == null) {
            adapter = byTenant.get(ApiAdapter.ALL_TENANTS);
        }
        if (adapter == null) {
            throw new IllegalStateException("No adapter registered for capability " + capability.getSimpleName()
                    + " and tenant '" + tenantId + "', and no default ('" + ApiAdapter.ALL_TENANTS + "') adapter exists.");
        }
        return (T) adapter;
    }
}
