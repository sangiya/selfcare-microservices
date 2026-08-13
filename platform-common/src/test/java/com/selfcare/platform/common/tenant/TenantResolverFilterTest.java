package com.selfcare.platform.common.tenant;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.concurrent.atomic.AtomicReference;
import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

class TenantResolverFilterTest {

    @AfterEach
    void tearDown() {
        TenantContext.clear();
        MDC.remove(TenantResolverFilter.MDC_TENANT_KEY);
    }

    @Test
    void usesHeaderTenantWhenPresent() throws Exception {
        TenantProperties properties = new TenantProperties();
        properties.setDefaultTenantId("default-tenant");
        TenantResolverFilter filter = new TenantResolverFilter(properties);
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader(properties.getHeaderName(), " acme-telecom ");
        AtomicReference<String> tenantInsideChain = new AtomicReference<>();
        AtomicReference<String> mdcTenantInsideChain = new AtomicReference<>();

        FilterChain chain = (req, res) -> {
            tenantInsideChain.set(TenantContext.get());
            mdcTenantInsideChain.set(MDC.get(TenantResolverFilter.MDC_TENANT_KEY));
        };

        filter.doFilter(request, new MockHttpServletResponse(), chain);

        assertThat(tenantInsideChain.get()).isEqualTo("acme-telecom");
        assertThat(mdcTenantInsideChain.get()).isEqualTo("acme-telecom");
        assertThat(TenantContext.getOrDefault("cleared")).isEqualTo("cleared");
        assertThat(MDC.get(TenantResolverFilter.MDC_TENANT_KEY)).isNull();
    }

    @Test
    void fallsBackToConfiguredDefaultTenantWhenHeaderIsMissing() throws Exception {
        TenantProperties properties = new TenantProperties();
        properties.setDefaultTenantId("default-tenant");
        TenantResolverFilter filter = new TenantResolverFilter(properties);
        AtomicReference<String> tenantInsideChain = new AtomicReference<>();

        FilterChain chain = (req, res) -> tenantInsideChain.set(TenantContext.get());

        filter.doFilter(new MockHttpServletRequest(), new MockHttpServletResponse(), chain);

        assertThat(tenantInsideChain.get()).isEqualTo("default-tenant");
    }
}
