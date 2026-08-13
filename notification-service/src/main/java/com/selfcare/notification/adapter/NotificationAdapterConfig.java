package com.selfcare.notification.adapter;

import com.selfcare.platform.common.adapter.ApiAdapterRegistry;
import jakarta.annotation.PostConstruct;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class NotificationAdapterConfig {

    private final ApiAdapterRegistry registry;
    private final List<NotificationProviderAdapter> adapters;

    public NotificationAdapterConfig(ApiAdapterRegistry registry, List<NotificationProviderAdapter> adapters) {
        this.registry = registry;
        this.adapters = adapters;
    }

    @PostConstruct
    void registerAdapters() {
        registry.register(NotificationProviderAdapter.class, adapters);
    }
}
