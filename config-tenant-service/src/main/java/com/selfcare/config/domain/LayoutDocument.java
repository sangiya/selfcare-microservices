package com.selfcare.config.domain;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.mapping.Document;

/**
 * The widget list for one operator's screen (Doc 1 sec 6.2 — "the Dashboard service does not
 * hard-code its widget list"). Adding/enabling a widget for an operator is an update to this
 * document, not a redeploy.
 */
@Document(collection = "tenant_layout")
@CompoundIndex(name = "tenant_screen_idx", def = "{'tenantId': 1, 'screenKey': 1}", unique = true)
public class LayoutDocument {

    @Id
    private String id;

    private String tenantId;

    /** e.g. "dashboard", "loyalty-home" */
    private String screenKey;

    private List<Widget> widgets;

    private int version = 1;

    @LastModifiedDate
    private Instant updatedAt;

    public LayoutDocument() {
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

    public List<Widget> getWidgets() {
        return widgets;
    }

    public void setWidgets(List<Widget> widgets) {
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

    public static class Widget {
        private String widgetId;
        private String type;
        private int order;
        private boolean enabled;
        /** Feature flag gating this widget on top of it being present in the layout. */
        private String featureFlag;
        private Map<String, Object> config;

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
