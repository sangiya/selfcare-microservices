package com.selfcare.platform.common.adapter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.selfcare.platform.common.tenant.TenantContext;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

class ApiAdapterRegistryTest {

    private final ApiAdapterRegistry registry = new ApiAdapterRegistry();

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    @Test
    void resolve_returnsTenantSpecificAdapterWhenPresent() {
        registry.register(ProbeAdapter.class, List.of(
                new TestAdapter(ApiAdapter.ALL_TENANTS, "default"),
                new TestAdapter("acme-telecom", "acme")));
        TenantContext.set("acme-telecom");

        ProbeAdapter adapter = registry.resolve(ProbeAdapter.class);

        assertThat(adapter.name()).isEqualTo("acme");
    }

    @Test
    void resolve_fallsBackToDefaultAdapter() {
        registry.register(ProbeAdapter.class, List.of(new TestAdapter(ApiAdapter.ALL_TENANTS, "default")));
        TenantContext.set("beta-telecom");

        ProbeAdapter adapter = registry.resolve(ProbeAdapter.class);

        assertThat(adapter.name()).isEqualTo("default");
    }

    @Test
    void resolve_failsWhenCapabilityHasNoRegisteredAdapters() {
        TenantContext.set("acme-telecom");

        assertThatThrownBy(() -> registry.resolve(ProbeAdapter.class))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("No adapters registered for capability ProbeAdapter");
    }

    @Test
    void resolve_failsWhenTenantSpecificAdapterExistsWithoutDefaultFallback() {
        registry.register(ProbeAdapter.class, List.of(new TestAdapter("other-tenant", "other")));
        TenantContext.set("acme-telecom");

        assertThatThrownBy(() -> registry.resolve(ProbeAdapter.class))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("tenant 'acme-telecom'")
                .hasMessageContaining(ApiAdapter.ALL_TENANTS);
    }

    private interface ProbeAdapter extends ApiAdapter {
        String name();
    }

    private record TestAdapter(String tenantId, String name) implements ProbeAdapter {}
}
