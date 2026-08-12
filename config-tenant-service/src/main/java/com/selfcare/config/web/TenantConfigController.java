package com.selfcare.config.web;

import com.selfcare.config.domain.LayoutDocument;
import com.selfcare.config.domain.TenantConfig;
import com.selfcare.config.service.TenantConfigService;
import com.selfcare.platform.common.web.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/tenants")
@Tag(name = "Tenant Config", description = "Doc 1 sec 6 — tenant resolution, theme/layout, and adapter bindings")
public class TenantConfigController {

    private final TenantConfigService tenantConfigService;

    public TenantConfigController(TenantConfigService tenantConfigService) {
        this.tenantConfigService = tenantConfigService;
    }

    @GetMapping("/{tenantId}")
    @Operation(summary = "Get an operator's full config by tenant ID")
    public ApiResponse<TenantConfig> getTenantConfig(@PathVariable String tenantId) {
        return ApiResponse.ok(tenantConfigService.getTenantConfig(tenantId));
    }

    @GetMapping("/resolve")
    @Operation(summary = "Resolve a tenant from a web host or a mobile app-flavor/API-key identifier")
    public ApiResponse<TenantConfig> resolve(
            @RequestParam(required = false) String host,
            @RequestParam(required = false) String appFlavorId) {
        if (host != null && !host.isBlank()) {
            return ApiResponse.ok(tenantConfigService.resolveByHost(host));
        }
        if (appFlavorId != null && !appFlavorId.isBlank()) {
            return ApiResponse.ok(tenantConfigService.resolveByAppFlavor(appFlavorId));
        }
        throw new com.selfcare.platform.common.web.BadRequestException("Provide either 'host' or 'appFlavorId'");
    }

    @GetMapping("/{tenantId}/layout/{screenKey}")
    @Operation(summary = "Get the widget layout for one operator's screen")
    public ApiResponse<LayoutDocument> getLayout(@PathVariable String tenantId, @PathVariable String screenKey) {
        return ApiResponse.ok(tenantConfigService.getLayout(tenantId, screenKey));
    }

    @PutMapping("/{tenantId}")
    @PreAuthorize("hasAuthority('SCOPE_platform-admin')")
    @Operation(summary = "Create/update an operator's config (platform-admin scope only)")
    public ApiResponse<TenantConfig> upsertTenantConfig(@PathVariable String tenantId, @Valid @RequestBody TenantConfig config) {
        config.setTenantId(tenantId);
        return ApiResponse.ok(tenantConfigService.upsertTenantConfig(config));
    }

    @PutMapping("/{tenantId}/layout/{screenKey}")
    @PreAuthorize("hasAuthority('SCOPE_platform-admin')")
    @Operation(summary = "Create/update an operator's screen layout (platform-admin scope only)")
    public ApiResponse<LayoutDocument> upsertLayout(
            @PathVariable String tenantId, @PathVariable String screenKey, @Valid @RequestBody LayoutDocument layout) {
        layout.setTenantId(tenantId);
        layout.setScreenKey(screenKey);
        return ApiResponse.ok(tenantConfigService.upsertLayout(layout));
    }
}
