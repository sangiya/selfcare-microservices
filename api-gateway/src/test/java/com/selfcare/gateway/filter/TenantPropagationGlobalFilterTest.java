package com.selfcare.gateway.filter;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.core.Ordered;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import reactor.core.publisher.Mono;

class TenantPropagationGlobalFilterTest {

    @Test
    void filter_preservesExplicitHeadersFromTheIncomingRequest() {
        TenantPropagationGlobalFilter filter = new TenantPropagationGlobalFilter("default-tenant");
        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/api/v1/balance")
                        .header(TenantPropagationGlobalFilter.TENANT_HEADER, "acme-telecom")
                        .header(TenantPropagationGlobalFilter.CORRELATION_HEADER, "trace-123")
                        .build());
        AtomicReference<ServerHttpRequest> forwardedRequest = new AtomicReference<>();
        GatewayFilterChain chain = ex -> {
            forwardedRequest.set(ex.getRequest());
            return Mono.empty();
        };

        filter.filter(exchange, chain).block();

        assertThat(forwardedRequest.get().getHeaders().getFirst(TenantPropagationGlobalFilter.TENANT_HEADER))
                .isEqualTo("acme-telecom");
        assertThat(forwardedRequest.get().getHeaders().getFirst(TenantPropagationGlobalFilter.CORRELATION_HEADER))
                .isEqualTo("trace-123");
    }

    @Test
    void filter_addsFallbackTenantAndGeneratedCorrelationIdWhenMissing() {
        TenantPropagationGlobalFilter filter = new TenantPropagationGlobalFilter("default-tenant");
        MockServerWebExchange exchange = MockServerWebExchange.from(MockServerHttpRequest.get("/health").build());
        AtomicReference<ServerHttpRequest> forwardedRequest = new AtomicReference<>();
        GatewayFilterChain chain = ex -> {
            forwardedRequest.set(ex.getRequest());
            return Mono.empty();
        };

        filter.filter(exchange, chain).block();

        assertThat(forwardedRequest.get().getHeaders().getFirst(TenantPropagationGlobalFilter.TENANT_HEADER))
                .isEqualTo("default-tenant");
        assertThat(forwardedRequest.get().getHeaders().getFirst(TenantPropagationGlobalFilter.CORRELATION_HEADER))
                .hasSize(36);
    }

    @Test
    void order_runsAtHighestPrecedence() {
        TenantPropagationGlobalFilter filter = new TenantPropagationGlobalFilter("default-tenant");

        assertThat(filter.getOrder()).isEqualTo(Ordered.HIGHEST_PRECEDENCE);
    }
}
