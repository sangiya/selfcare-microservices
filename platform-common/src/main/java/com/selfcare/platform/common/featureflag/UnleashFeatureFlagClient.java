package com.selfcare.platform.common.featureflag;

import com.selfcare.platform.common.tenant.TenantContext;
import io.getunleash.Unleash;
import io.getunleash.UnleashContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Default {@link FeatureFlagClient} backed by the self-hosted Unleash instance (Doc 2 sec 3.9).
 * The current tenant is passed as the Unleash "userId"/context field so operator-level
 * strategies (e.g. "enabled for these 3 operators only") work out of the box.
 */
public class UnleashFeatureFlagClient implements FeatureFlagClient {

    private static final Logger log = LoggerFactory.getLogger(UnleashFeatureFlagClient.class);

    private final Unleash unleash;

    public UnleashFeatureFlagClient(Unleash unleash) {
        this.unleash = unleash;
    }

    @Override
    public boolean isEnabled(String flagName, boolean defaultValue) {
        try {
            UnleashContext context = UnleashContext.builder()
                    .userId(TenantContext.getOrDefault("unknown"))
                    .addProperty("tenantId", TenantContext.getOrDefault("unknown"))
                    .build();
            return unleash.isEnabled(flagName, context, defaultValue);
        } catch (Exception ex) {
            // Fail according to the caller's stated default rather than letting a flag-service
            // outage take down every gated code path — see FeatureFlagClient's javadoc.
            log.warn("Feature flag '{}' evaluation failed, falling back to default={}", flagName, defaultValue, ex);
            return defaultValue;
        }
    }
}
