package com.selfcare.gateway.filter;

import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

/**
 * Reactive equivalent of platform-common's {@code TenantResolverFilter} (Spring Cloud Gateway
 * runs on WebFlux, so the servlet-based filter in platform-common doesn't apply here). Ensures
 * every request forwarded downstream carries {@code X-Tenant-Id} and {@code X-Correlation-Id},
 * so every microservice behind the gateway can trust those headers are always present.
 *
 * <p>Resolution order matches Doc 1 sec 6.1: an explicit header from the client/app build wins;
 * otherwise this gateway instance's own configured tenant is used — the normal case for a
 * per-operator gateway deployment (Doc 1 sec 5). To resolve tenant by host instead (a gateway
 * shared across operators), call config-tenant-service's {@code /api/v1/tenants/resolve}
 * endpoint here instead of falling back to the static default — left as an extension point
 * since it adds a network hop on every request that most deployments won't need.
 */
@Component
public class TenantPropagationGlobalFilter implements GlobalFilter, Ordered {

    public static final String TENANT_HEADER = "X-Tenant-Id";
    public static final String CORRELATION_HEADER = "X-Correlation-Id";

    private final String defaultTenantId;

    public TenantPropagationGlobalFilter(@Value("${platform.tenant.default-tenant-id:default}") String defaultTenantId) {
        this.defaultTenantId = defaultTenantId;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();

        String tenantId = request.getHeaders().getFirst(TENANT_HEADER);
        if (tenantId == null || tenantId.isBlank()) {
            tenantId = defaultTenantId;
        }

        String correlationId = request.getHeaders().getFirst(CORRELATION_HEADER);
        if (correlationId == null || correlationId.isBlank()) {
            correlationId = UUID.randomUUID().toString();
        }

        ServerHttpRequest mutatedRequest = request.mutate()
                .header(TENANT_HEADER, tenantId)
                .header(CORRELATION_HEADER, correlationId)
                .build();

        return chain.filter(exchange.mutate().request(mutatedRequest).build());
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE;
    }
}
