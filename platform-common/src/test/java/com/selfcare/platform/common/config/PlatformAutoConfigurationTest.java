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
import com.sun.net.httpserver.HttpServer;
import io.getunleash.DefaultUnleash;
import io.getunleash.Unleash;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.AfterEach;
import org.springframework.boot.web.servlet.FilterRegistrationBean;

class PlatformAutoConfigurationTest {

    private final PlatformAutoConfiguration configuration = new PlatformAutoConfiguration();
    private HttpServer server;

    @AfterEach
    void tearDown() {
        if (server != null) {
            server.stop(0);
        }
    }

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
    void unleashClient_buildsAConfiguredClient() throws Exception {
        startUnleashStub();

        Unleash unleash = configuration.unleashClient(baseUrl() + "/api", "platform-common-test", "");

        assertThat(unleash).isInstanceOf(DefaultUnleash.class);
        ((DefaultUnleash) unleash).shutdown();
    }

    @Test
    void unleashClient_appliesApiTokenWhenConfigured() throws Exception {
        startUnleashStub();

        Unleash unleash = configuration.unleashClient(baseUrl() + "/api", "platform-common-test", "local-dev-admin-token");

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

    private void startUnleashStub() throws IOException {
        server = HttpServer.create(new InetSocketAddress(0), 0);
        server.createContext("/api/client/features", exchange -> {
            writeResponse(exchange, 200, "{\"version\":1,\"features\":[]}");
            exchange.close();
        });
        server.createContext("/api/client/register", exchange -> {
            writeResponse(exchange, 202, "{}");
            exchange.close();
        });
        server.createContext("/api/client/metrics", exchange -> {
            writeResponse(exchange, 202, "{}");
            exchange.close();
        });
        server.start();
    }

    private String baseUrl() {
        return "http://localhost:" + server.getAddress().getPort();
    }

    private static void writeResponse(com.sun.net.httpserver.HttpExchange exchange, int status, String body)
            throws IOException {
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().add("Content-Type", "application/json");
        exchange.sendResponseHeaders(status, bytes.length);
        try (OutputStream outputStream = exchange.getResponseBody()) {
            outputStream.write(bytes);
        }
    }
}
