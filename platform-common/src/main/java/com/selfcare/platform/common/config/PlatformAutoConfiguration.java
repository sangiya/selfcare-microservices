package com.selfcare.platform.common.config;

import com.selfcare.platform.common.adapter.ApiAdapterRegistry;
import com.selfcare.platform.common.featureflag.FeatureFlagClient;
import com.selfcare.platform.common.featureflag.UnleashFeatureFlagClient;
import com.selfcare.platform.common.observability.CorrelationIdFilter;
import com.selfcare.platform.common.tenant.TenantProperties;
import com.selfcare.platform.common.tenant.TenantResolverFilter;
import io.getunleash.DefaultUnleash;
import io.getunleash.Unleash;
import io.getunleash.util.UnleashConfig;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.util.StringUtils;

/**
 * Wires every cross-cutting concern (tenant resolution, correlation IDs, the API Adapter
 * Registry, feature flags) into each service automatically the moment it depends on
 * platform-common — nothing here needs to be imported/configured by hand in a downstream
 * service beyond setting the relevant application.yml properties. See this module's README
 * section in the root README.md for the full property list.
 */
@AutoConfiguration
@EnableConfigurationProperties(TenantProperties.class)
public class PlatformAutoConfiguration {

    @Bean
    public FilterRegistrationBean<TenantResolverFilter> tenantResolverFilter(TenantProperties tenantProperties) {
        FilterRegistrationBean<TenantResolverFilter> registration =
                new FilterRegistrationBean<>(new TenantResolverFilter(tenantProperties));
        registration.setOrder(1);
        registration.addUrlPatterns("/*");
        return registration;
    }

    @Bean
    public FilterRegistrationBean<CorrelationIdFilter> correlationIdFilter() {
        FilterRegistrationBean<CorrelationIdFilter> registration =
                new FilterRegistrationBean<>(new CorrelationIdFilter());
        registration.setOrder(0);
        registration.addUrlPatterns("/*");
        return registration;
    }

    @Bean
    @ConditionalOnMissingBean
    public ApiAdapterRegistry apiAdapterRegistry() {
        return new ApiAdapterRegistry();
    }

    @Bean
    @ConditionalOnExpression("T(org.springframework.util.StringUtils).hasText('${platform.feature-flags.api-url:}')")
    public Unleash unleashClient(
            @Value("${platform.feature-flags.api-url}") String apiUrl,
            @Value("${spring.application.name}") String appName,
            @Value("${platform.feature-flags.api-token:}") String apiToken) {
        UnleashConfig.Builder configBuilder = UnleashConfig.builder()
                .appName(appName)
                .unleashAPI(apiUrl);
        if (StringUtils.hasText(apiToken)) {
            // Unleash's client API rejects unauthenticated polling with a 401 -- see the
            // Authorization header requirement in Unleash's client SDK docs. Without this, every
            // service pointed at a real Unleash instance backs off and falls through to the
            // caller-supplied default on every flag check.
            configBuilder.apiKey(apiToken);
        }
        return new DefaultUnleash(configBuilder.build());
    }

    @Bean
    @ConditionalOnExpression("T(org.springframework.util.StringUtils).hasText('${platform.feature-flags.api-url:}')")
    public FeatureFlagClient unleashFeatureFlagClient(Unleash unleash) {
        return new UnleashFeatureFlagClient(unleash);
    }

    /**
     * When no Unleash endpoint is configured (e.g. local dev, or a service that hasn't wired
     * flags yet), every flag check simply returns its caller-supplied default rather than
     * failing service startup — keeps the framework usable incrementally.
     */
    @Bean
    @ConditionalOnMissingBean(FeatureFlagClient.class)
    public FeatureFlagClient noOpFeatureFlagClient() {
        return (flagName, defaultValue) -> defaultValue;
    }
}
