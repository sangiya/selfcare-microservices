package com.selfcare.config.web.dto;

import com.selfcare.config.domain.LayoutDocument;
import java.time.Instant;
import java.util.List;
import java.util.Map;

public class LayoutDocumentDto {

    private String id;
    private String tenantId;
    private String screenKey;
    private List<WidgetDto> widgets;
    private int version = 1;
    private Instant updatedAt;

    public static LayoutDocumentDto fromDomain(LayoutDocument domain) {
        LayoutDocumentDto dto = new LayoutDocumentDto();
        dto.setId(domain.getId());
        dto.setTenantId(domain.getTenantId());
        dto.setScreenKey(domain.getScreenKey());
        dto.setWidgets(domain.getWidgets() == null ? null : domain.getWidgets().stream().map(WidgetDto::fromDomain).toList());
        dto.setVersion(domain.getVersion());
        dto.setUpdatedAt(domain.getUpdatedAt());
        return dto;
    }

    public LayoutDocument toDomain() {
        LayoutDocument domain = new LayoutDocument();
        domain.setId(id);
        domain.setTenantId(tenantId);
        domain.setScreenKey(screenKey);
        domain.setWidgets(widgets == null ? null : widgets.stream().map(WidgetDto::toDomain).toList());
        domain.setVersion(version);
        return domain;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getTenantId() {
        return tenantId;
    }

    public void setTenantId(String tenantId) {
        this.tenantId = tenantId;
    }

    public String getScreenKey() {
        return screenKey;
    }

    public void setScreenKey(String screenKey) {
        this.screenKey = screenKey;
    }

    public List<WidgetDto> getWidgets() {
        return widgets;
    }

    public void setWidgets(List<WidgetDto> widgets) {
        this.widgets = widgets;
    }

    public int getVersion() {
        return version;
    }

    public void setVersion(int version) {
        this.version = version;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }

    public static class WidgetDto {
        private String widgetId;
        private String type;
        private int order;
        private boolean enabled;
        private String featureFlag;
        private Map<String, Object> config;

        public static WidgetDto fromDomain(LayoutDocument.Widget domain) {
            WidgetDto dto = new WidgetDto();
            dto.setWidgetId(domain.getWidgetId());
            dto.setType(domain.getType());
            dto.setOrder(domain.getOrder());
            dto.setEnabled(domain.isEnabled());
            dto.setFeatureFlag(domain.getFeatureFlag());
            dto.setConfig(domain.getConfig());
            return dto;
        }

        public LayoutDocument.Widget toDomain() {
            LayoutDocument.Widget domain = new LayoutDocument.Widget();
            domain.setWidgetId(widgetId);
            domain.setType(type);
            domain.setOrder(order);
            domain.setEnabled(enabled);
            domain.setFeatureFlag(featureFlag);
            domain.setConfig(config);
            return domain;
        }

        public String getWidgetId() {
            return widgetId;
        }

        public void setWidgetId(String widgetId) {
            this.widgetId = widgetId;
        }

        public String getType() {
            return type;
        }

        public void setType(String type) {
            this.type = type;
        }

        public int getOrder() {
            return order;
        }

        public void setOrder(int order) {
            this.order = order;
        }

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public String getFeatureFlag() {
            return featureFlag;
        }

        public void setFeatureFlag(String featureFlag) {
            this.featureFlag = featureFlag;
        }

        public Map<String, Object> getConfig() {
            return config;
        }

        public void setConfig(Map<String, Object> config) {
            this.config = config;
        }
    }
}
