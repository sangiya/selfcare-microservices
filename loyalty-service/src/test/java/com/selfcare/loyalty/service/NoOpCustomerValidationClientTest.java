package com.selfcare.loyalty.service;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class NoOpCustomerValidationClientTest {

    @Test
    void alwaysTreatsEveryCustomerAsValidInDevMode() {
        NoOpCustomerValidationClient client = new NoOpCustomerValidationClient();

        assertThat(client.isValidCustomer("912345678V", "94771234567")).isTrue();
    }

    @Test
    void warnDevModeIsCallableAtStartup() {
        NoOpCustomerValidationClient client = new NoOpCustomerValidationClient();

        client.warnDevMode();

        assertThat(client.isValidCustomer("anything", "anything")).isTrue();
    }
}
