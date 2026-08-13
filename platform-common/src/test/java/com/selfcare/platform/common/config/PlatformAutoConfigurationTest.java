package com.selfcare.platform.common.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.selfcare.platform.common.adapter.ApiAdapterRegistry;
import com.selfcare.platform.common.featureflag.FeatureFlagClient;
import com.selfcare.platform.common.featureflag.UnleashFeatureFlagClient;
import com.selfcare.platform.common.observability.CorrelationIdFilter;
import com.selfcare.platform.common.tenant.TenantProperties;
import com.selfcare.platform.common.tenant.TenantResolverFilter;
import io.getunleash.DefaultUnleash;
import io.getunleash.Unleash;
import org.junit.jupiter.api.Test;
import org.springframework.boot.web.servlet.FilterRegistrationBean;

class PlatformAutoConfigurationTest {

    private final PlatformAutoConfiguration configuration = new PlatformAutoConfiguration();

    @Test
    void tenantResolverFilter_registersTheExpectedFilter() {
        TenantProperties properties = new TenantProperties();

        FilterRegistrationBean<TenantResolverFilter> registration = configuration.tenantResolverFilter(properties);

        assertThat(registration.getFilter()).isInstanceOf(TenantResolverFilter.class);
        assertThat(registration.getOrder()).isEqualTo(1);
        assertThat(registration.getUrlPatterns()).containsExactly("/*");
    }

    @Test
    void correlationIdFilter_registersTheExpectedFilter() {
        FilterRegistrationBean<CorrelationIdFilter> registration = configuration.correlationIdFilter();

        assertThat(registration.getFilter()).isInstanceOf(CorrelationIdFilter.class);
        assertThat(registration.getOrder()).isZero();
        assertThat(registration.getUrlPatterns()).containsExactly("/*");
    }

    @Test
    void apiAdapterRegistry_createsTheDefaultRegistryBean() {
        assertThat(configuration.apiAdapterRegistry()).isInstanceOf(ApiAdapterRegistry.class);
    }

    @Test
    void unleashClient_buildsAConfiguredClient() {
        Unleash unleash = configuration.unleashClient("http://localhost:4242/api", "platform-common-test");

        assertThat(unleash).isInstanceOf(DefaultUnleash.class);
        ((DefaultUnleash) unleash).shutdown();
    }

    @Test
    void unleashFeatureFlagClient_wrapsTheProvidedUnleashClient() {
        Unleash unleash = mock(Unleash.class);
        when(unleash.isEnabled(eq("pilot-flag"), org.mockito.ArgumentMatchers.any(), eq(false))).thenReturn(true);

        FeatureFlagClient client = configuration.unleashFeatureFlagClient(unleash);

        assertThat(client).isInstanceOf(UnleashFeatureFlagClient.class);
        assertThat(client.isEnabled("pilot-flag", false)).isTrue();
    }

    @Test
    void noOpFeatureFlagClient_returnsTheCallerSuppliedDefault() {
        FeatureFlagClient client = configuration.noOpFeatureFlagClient();

        assertThat(client.isEnabled("missing-flag", true)).isTrue();
        assertThat(client.isEnabled("missing-flag", false)).isFalse();
    }
}
