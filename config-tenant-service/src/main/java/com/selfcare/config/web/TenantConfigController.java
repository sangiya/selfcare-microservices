package com.selfcare.config.web;

import com.selfcare.config.domain.LayoutDocument;
import com.selfcare.config.domain.TenantConfig;
import com.selfcare.config.service.TenantConfigService;
import com.selfcare.config.web.dto.LayoutDocumentDto;
import com.selfcare.config.web.dto.TenantConfigDto;
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
    public ApiResponse<TenantConfigDto> getTenantConfig(@PathVariable String tenantId) {
        return ApiResponse.ok(TenantConfigDto.fromDomain(tenantConfigService.getTenantConfig(tenantId)));
    }

    @GetMapping("/resolve")
    @Operation(summary = "Resolve a tenant from a web host or a mobile app-flavor/API-key identifier")
    public ApiResponse<TenantConfigDto> resolve(
            @RequestParam(required = false) String host,
            @RequestParam(required = false) String appFlavorId) {
        if (host != null && !host.isBlank()) {
            return ApiResponse.ok(TenantConfigDto.fromDomain(tenantConfigService.resolveByHost(host)));
        }
        if (appFlavorId != null && !appFlavorId.isBlank()) {
            return ApiResponse.ok(TenantConfigDto.fromDomain(tenantConfigService.resolveByAppFlavor(appFlavorId)));
        }
        throw new com.selfcare.platform.common.web.BadRequestException("Provide either 'host' or 'appFlavorId'");
    }

    @GetMapping("/{tenantId}/layout/{screenKey}")
    @Operation(summary = "Get the widget layout for one operator's screen")
    public ApiResponse<LayoutDocumentDto> getLayout(@PathVariable String tenantId, @PathVariable String screenKey) {
        return ApiResponse.ok(LayoutDocumentDto.fromDomain(tenantConfigService.getLayout(tenantId, screenKey)));
    }

    @PutMapping("/{tenantId}")
    @PreAuthorize("hasAuthority('SCOPE_platform-admin')")
    @Operation(summary = "Create/update an operator's config (platform-admin scope only)")
    public ApiResponse<TenantConfigDto> upsertTenantConfig(
            @PathVariable String tenantId,
            @Valid @RequestBody TenantConfigDto config) {
        config.setTenantId(tenantId);
        TenantConfig saved = tenantConfigService.upsertTenantConfig(config.toDomain());
        return ApiResponse.ok(TenantConfigDto.fromDomain(saved));
    }

    @PutMapping("/{tenantId}/layout/{screenKey}")
    @PreAuthorize("hasAuthority('SCOPE_platform-admin')")
    @Operation(summary = "Create/update an operator's screen layout (platform-admin scope only)")
    public ApiResponse<LayoutDocumentDto> upsertLayout(
            @PathVariable String tenantId,
            @PathVariable String screenKey,
            @Valid @RequestBody LayoutDocumentDto layout) {
        layout.setTenantId(tenantId);
        layout.setScreenKey(screenKey);
        LayoutDocument saved = tenantConfigService.upsertLayout(layout.toDomain());
        return ApiResponse.ok(LayoutDocumentDto.fromDomain(saved));
    }
}
