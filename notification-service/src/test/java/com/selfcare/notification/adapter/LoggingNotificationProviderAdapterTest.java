package com.selfcare.notification.adapter;

import static org.assertj.core.api.Assertions.assertThat;

import com.selfcare.notification.domain.NotificationChannel;
import com.selfcare.notification.domain.NotificationRequest;
import com.selfcare.notification.domain.NotificationStatus;
import com.selfcare.platform.common.adapter.ApiAdapter;
import org.junit.jupiter.api.Test;

class LoggingNotificationProviderAdapterTest {

    private final LoggingNotificationProviderAdapter adapter = new LoggingNotificationProviderAdapter();

    @Test
    void send_marksNotificationsAsSent() {
        NotificationRequest request = new NotificationRequest();
        request.setTenantId("acme-telecom");
        request.setSubscriberMsisdn("94771234567");
        request.setChannel(NotificationChannel.SMS);
        request.setTemplateKey("welcome-sms");
        request.setSourceEvent("direct-api");

        var result = adapter.send(request);

        assertThat(result.status()).isEqualTo(NotificationStatus.SENT);
    }

    @Test
    void tenantId_registersTheAdapterAsTheDefaultProvider() {
        assertThat(adapter.tenantId()).isEqualTo(ApiAdapter.ALL_TENANTS);
    }
}
