package com.selfcare.platform.common.tenant;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class TenantPropertiesTest {

    @Test
    void exposesSensibleDefaultsAndMutableOverrides() {
        TenantProperties properties = new TenantProperties();

        assertThat(properties.getDefaultTenantId()).isEqualTo("default");
        assertThat(properties.getHeaderName()).isEqualTo("X-Tenant-Id");

        properties.setDefaultTenantId("acme-telecom");
        properties.setHeaderName("X-Operator");

        assertThat(properties.getDefaultTenantId()).isEqualTo("acme-telecom");
        assertThat(properties.getHeaderName()).isEqualTo("X-Operator");
    }
}
