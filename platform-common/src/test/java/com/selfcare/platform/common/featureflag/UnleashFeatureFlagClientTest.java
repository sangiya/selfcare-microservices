package com.selfcare.platform.common.featureflag;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.selfcare.platform.common.tenant.TenantContext;
import io.getunleash.Unleash;
import io.getunleash.UnleashContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class UnleashFeatureFlagClientTest {

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    @Test
    void isEnabled_passesCurrentTenantIntoTheUnleashContext() {
        Unleash unleash = org.mockito.Mockito.mock(Unleash.class);
        when(unleash.isEnabled(eq("partner-transfer"), any(UnleashContext.class), eq(false))).thenReturn(true);
        TenantContext.set("acme-telecom");

        UnleashFeatureFlagClient client = new UnleashFeatureFlagClient(unleash);
        boolean enabled = client.isEnabled("partner-transfer", false);

        ArgumentCaptor<UnleashContext> contextCaptor = ArgumentCaptor.forClass(UnleashContext.class);
        verify(unleash).isEnabled(eq("partner-transfer"), contextCaptor.capture(), eq(false));
        assertThat(enabled).isTrue();
        assertThat(contextCaptor.getValue().getUserId()).contains("acme-telecom");
        assertThat(contextCaptor.getValue().getProperties()).containsEntry("tenantId", "acme-telecom");
    }

    @Test
    void isEnabled_fallsBackToUnknownTenantWhenNoContextIsSet() {
        Unleash unleash = org.mockito.Mockito.mock(Unleash.class);
        when(unleash.isEnabled(eq("partner-transfer"), any(UnleashContext.class), eq(true))).thenReturn(false);

        UnleashFeatureFlagClient client = new UnleashFeatureFlagClient(unleash);
        client.isEnabled("partner-transfer", true);

        ArgumentCaptor<UnleashContext> contextCaptor = ArgumentCaptor.forClass(UnleashContext.class);
        verify(unleash).isEnabled(eq("partner-transfer"), contextCaptor.capture(), eq(true));
        assertThat(contextCaptor.getValue().getUserId()).contains("unknown");
        assertThat(contextCaptor.getValue().getProperties()).containsEntry("tenantId", "unknown");
    }

    @Test
    void isEnabled_returnsDefaultValueWhenUnleashThrows() {
        Unleash unleash = org.mockito.Mockito.mock(Unleash.class);
        when(unleash.isEnabled(eq("partner-transfer"), any(UnleashContext.class), eq(true)))
                .thenThrow(new IllegalStateException("unleash unavailable"));

        UnleashFeatureFlagClient client = new UnleashFeatureFlagClient(unleash);

        assertThat(client.isEnabled("partner-transfer", true)).isTrue();
    }
}
