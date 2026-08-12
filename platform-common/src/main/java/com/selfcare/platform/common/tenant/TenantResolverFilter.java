package com.selfcare.platform.common.tenant;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.slf4j.MDC;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Resolves the operator/tenant for every inbound request (Doc 1 sec 6.1) and makes it
 * available via {@link TenantContext} for the lifetime of the request. Resolution order:
 *
 * <ol>
 *   <li>Explicit {@code X-Tenant-Id} header (set by the API Gateway/BFF once it has resolved
 *       the tenant from host/app-flavor/API key for a shared-gateway deployment).</li>
 *   <li>This service's own configured default tenant ({@code platform.tenant.default-tenant-id}),
 *       which is how single-operator, independently-deployed instances behave (Doc 1 sec 5) —
 *       no header is required in that model since the whole deployment belongs to one operator.</li>
 * </ol>
 *
 * <p>The resolved tenant is also written to the logging MDC so every log line — shipped to the
 * existing ELK stack (Doc 2 sec 4) — is attributable to an operator without extra plumbing.
 */
public class TenantResolverFilter extends OncePerRequestFilter {

    public static final String MDC_TENANT_KEY = "tenantId";

    private final TenantProperties tenantProperties;

    public TenantResolverFilter(TenantProperties tenantProperties) {
        this.tenantProperties = tenantProperties;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String headerTenant = request.getHeader(tenantProperties.getHeaderName());
        String tenantId = (headerTenant != null && !headerTenant.isBlank())
                ? headerTenant.trim()
                : tenantProperties.getDefaultTenantId();

        TenantContext.set(tenantId);
        MDC.put(MDC_TENANT_KEY, tenantId);
        try {
            filterChain.doFilter(request, response);
        } finally {
            TenantContext.clear();
            MDC.remove(MDC_TENANT_KEY);
        }
    }
}
