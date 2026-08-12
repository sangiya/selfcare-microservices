package com.selfcare.config.domain;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

/**
 * One operator's full configuration document (Doc 1 sec 6). This is the artifact a new
 * operator onboarding produces (Doc 1 sec 5, step 3) — everything below is a config write,
 * never a code change.
 */
@Document(collection = "tenant_config")
public class TenantConfig {

    @Id
    private String tenantId;

    private String operatorDisplayName;

    /** Web hostnames that resolve to this tenant, e.g. ["selfcare.acme-telecom.com"]. */
    @Indexed
    private List<String> hostAliases;

    /** Mobile app build/API-key identifiers that resolve to this tenant. */
    @Indexed
    private List<String> appFlavorIds;

    private ThemeConfig theme;

    /**
     * Named capability -> which adapter implementation this operator uses, e.g.
     * {"loyalty-core": "mife", "sms-provider": "twilio"}. Read by services at startup/refresh
     * to decide adapter wiring alongside the API Adapter Registry (Doc 1 sec 6.3).
     */
    private Map<String, String> apiAdapterBindings;

    /** Unleash flag names force-enabled for this tenant regardless of global default. */
    private List<String> enabledFeatureFlags;

    private boolean active = true;

    @CreatedDate
    private Instant createdAt;

    @LastModifiedDate
    private Instant updatedAt;

    public TenantConfig() {
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

    public ThemeConfig getTheme() {
        return theme;
    }

    public void setTheme(ThemeConfig theme) {
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

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    /** Branding/theme fields consumed directly by the RN app and web-view (Doc 1 sec 7). */
    public static class ThemeConfig {
        private String primaryColor;
        private String secondaryColor;
        private String logoUrl;
        private String appDisplayName;

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
