package com.selfcare.platform.common.tenant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

class TenantContextTest {

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    @Test
    void get_returnsTheCurrentTenant() {
        TenantContext.set("acme-telecom");

        assertThat(TenantContext.get()).isEqualTo("acme-telecom");
    }

    @Test
    void get_throwsWhenNoTenantWasResolved() {
        assertThatThrownBy(TenantContext::get)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("No tenant resolved for this request");
    }

    @Test
    void get_throwsWhenTenantIsBlank() {
        TenantContext.set(" ");

        assertThatThrownBy(TenantContext::get)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("No tenant resolved for this request");
    }

    @Test
    void getOrDefault_returnsFallbackWhenTenantIsMissingOrBlank() {
        assertThat(TenantContext.getOrDefault("fallback")).isEqualTo("fallback");

        TenantContext.set(" ");

        assertThat(TenantContext.getOrDefault("fallback")).isEqualTo("fallback");
    }

    @Test
    void clear_removesTheCurrentTenant() {
        TenantContext.set("acme-telecom");

        TenantContext.clear();

        assertThat(TenantContext.getOrDefault("fallback")).isEqualTo("fallback");
    }
}
