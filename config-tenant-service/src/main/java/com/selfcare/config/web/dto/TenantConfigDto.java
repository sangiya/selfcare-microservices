package com.selfcare.config.web.dto;

import com.selfcare.config.domain.TenantConfig;
import java.time.Instant;
import java.util.List;
import java.util.Map;

public class TenantConfigDto {

    private String tenantId;
    private String operatorDisplayName;
    private List<String> hostAliases;
    private List<String> appFlavorIds;
    private ThemeConfigDto theme;
    private Map<String, String> apiAdapterBindings;
    private List<String> enabledFeatureFlags;
    private boolean active = true;
    private Instant createdAt;
    private Instant updatedAt;

    public static TenantConfigDto fromDomain(TenantConfig domain) {
        TenantConfigDto dto = new TenantConfigDto();
        dto.setTenantId(domain.getTenantId());
        dto.setOperatorDisplayName(domain.getOperatorDisplayName());
        dto.setHostAliases(domain.getHostAliases());
        dto.setAppFlavorIds(domain.getAppFlavorIds());
        dto.setTheme(ThemeConfigDto.fromDomain(domain.getTheme()));
        dto.setApiAdapterBindings(domain.getApiAdapterBindings());
        dto.setEnabledFeatureFlags(domain.getEnabledFeatureFlags());
        dto.setActive(domain.isActive());
        dto.setCreatedAt(domain.getCreatedAt());
        dto.setUpdatedAt(domain.getUpdatedAt());
        return dto;
    }

    public TenantConfig toDomain() {
        TenantConfig domain = new TenantConfig();
        domain.setTenantId(tenantId);
        domain.setOperatorDisplayName(operatorDisplayName);
        domain.setHostAliases(hostAliases);
        domain.setAppFlavorIds(appFlavorIds);
        domain.setTheme(theme == null ? null : theme.toDomain());
        domain.setApiAdapterBindings(apiAdapterBindings);
        domain.setEnabledFeatureFlags(enabledFeatureFlags);
        domain.setActive(active);
        return domain;
    }

    public String getTenantId() {
        return tenantId;
    }

    public void setTenantId(String tenantId) {
        this.tenantId = tenantId;
    }

    public String getOperatorDisplayName() {
        return operatorDisplayName;
    }

    public void setOperatorDisplayName(String operatorDisplayName) {
        this.operatorDisplayName = operatorDisplayName;
    }

    public List<String> getHostAliases() {
        return hostAliases;
    }

    public void setHostAliases(List<String> hostAliases) {
        this.hostAliases = hostAliases;
    }

    public List<String> getAppFlavorIds() {
        return appFlavorIds;
    }

    public void setAppFlavorIds(List<String> appFlavorIds) {
        this.appFlavorIds = appFlavorIds;
    }

    public ThemeConfigDto getTheme() {
        return theme;
    }

    public void setTheme(ThemeConfigDto theme) {
        this.theme = theme;
    }

    public Map<String, String> getApiAdapterBindings() {
        return apiAdapterBindings;
    }

    public void setApiAdapterBindings(Map<String, String> apiAdapterBindings) {
        this.apiAdapterBindings = apiAdapterBindings;
    }

    public List<String> getEnabledFeatureFlags() {
        return enabledFeatureFlags;
    }

    public void setEnabledFeatureFlags(List<String> enabledFeatureFlags) {
        this.enabledFeatureFlags = enabledFeatureFlags;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }

    public static class ThemeConfigDto {
        private String primaryColor;
        private String secondaryColor;
        private String logoUrl;
        private String appDisplayName;

        public static ThemeConfigDto fromDomain(TenantConfig.ThemeConfig domain) {
            if (domain == null) {
                return null;
            }
            ThemeConfigDto dto = new ThemeConfigDto();
            dto.setPrimaryColor(domain.getPrimaryColor());
            dto.setSecondaryColor(domain.getSecondaryColor());
            dto.setLogoUrl(domain.getLogoUrl());
            dto.setAppDisplayName(domain.getAppDisplayName());
            return dto;
        }

        public TenantConfig.ThemeConfig toDomain() {
            TenantConfig.ThemeConfig domain = new TenantConfig.ThemeConfig();
            domain.setPrimaryColor(primaryColor);
            domain.setSecondaryColor(secondaryColor);
            domain.setLogoUrl(logoUrl);
            domain.setAppDisplayName(appDisplayName);
            return domain;
        }

        public String getPrimaryColor() {
            return primaryColor;
        }

        public void setPrimaryColor(String primaryColor) {
            this.primaryColor = primaryColor;
        }

        public String getSecondaryColor() {
            return secondaryColor;
        }

        public void setSecondaryColor(String secondaryColor) {
            this.secondaryColor = secondaryColor;
        }

        public String getLogoUrl() {
            return logoUrl;
        }

        public void setLogoUrl(String logoUrl) {
            this.logoUrl = logoUrl;
        }

        public String getAppDisplayName() {
            return appDisplayName;
        }

        public void setAppDisplayName(String appDisplayName) {
            this.appDisplayName = appDisplayName;
        }
    }
}
