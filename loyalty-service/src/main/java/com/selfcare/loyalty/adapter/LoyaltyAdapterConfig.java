package com.selfcare.loyalty.adapter;

import com.selfcare.platform.common.adapter.ApiAdapterRegistry;
import jakarta.annotation.PostConstruct;
import java.util.List;
import org.springframework.stereotype.Component;

/**
 * Registers every {@link LoyaltyCoreAdapter} bean (the MIFE default, plus any operator-specific
 * overrides added later -- see the javadoc on {@link MifeLoyaltyCoreAdapter}) into the shared
 * {@link ApiAdapterRegistry} at startup. This is the only place a new adapter implementation
 * needs to be picked up automatically by Spring's {@code List<LoyaltyCoreAdapter>} injection.
 */
@Component
public class LoyaltyAdapterConfig {

    private final ApiAdapterRegistry registry;
    private final List<LoyaltyCoreAdapter> adapters;

    public LoyaltyAdapterConfig(ApiAdapterRegistry registry, List<LoyaltyCoreAdapter> adapters) {
        this.registry = registry;
        this.adapters = adapters;
    }

    @PostConstruct
    void registerAdapters() {
        registry.register(LoyaltyCoreAdapter.class, adapters);
    }
}
