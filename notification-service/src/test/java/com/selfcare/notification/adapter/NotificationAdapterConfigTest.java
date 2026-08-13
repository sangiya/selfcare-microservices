package com.selfcare.notification.adapter;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import com.selfcare.platform.common.adapter.ApiAdapterRegistry;
import java.util.List;
import org.junit.jupiter.api.Test;

class NotificationAdapterConfigTest {

    @Test
    void registerAdapters_registersEveryProviderAdapterInTheRegistry() {
        ApiAdapterRegistry registry = mock(ApiAdapterRegistry.class);
        NotificationProviderAdapter adapter = mock(NotificationProviderAdapter.class);
        NotificationAdapterConfig config = new NotificationAdapterConfig(registry, List.of(adapter));

        config.registerAdapters();

        verify(registry).register(NotificationProviderAdapter.class, List.of(adapter));
    }
}
