package com.selfcare.notification.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.selfcare.notification.domain.NotificationChannel;
import com.selfcare.notification.domain.NotificationRequest;
import com.selfcare.notification.repository.NotificationRequestRepository;
import com.selfcare.notification.service.NotificationDeliveryService;
import com.selfcare.platform.common.tenant.TenantContext;
import com.selfcare.platform.common.web.ApiResponse;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Unit tests for the synchronous "request a notification directly" path. Delivery is delegated
 * to a service so the controller can focus on request shaping and tenant/source tagging.
 */
@ExtendWith(MockitoExtension.class)
class NotificationControllerTest {

    private static final String TENANT = "acme-telecom";
    private static final String MSISDN = "94771234567";

    @Mock
    private NotificationRequestRepository repository;

    private NotificationController controller;
    private RecordingNotificationDeliveryService deliveryService;

    @BeforeEach
    void setUp() {
        TenantContext.set(TENANT);
        deliveryService = new RecordingNotificationDeliveryService();
        controller = new NotificationController(deliveryService, repository);
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    @Test
    void send_delegatesTenantScopedRequestTaggedAsDirectApiToDeliveryService() {
        ApiResponse<NotificationRequest> response = controller.send(MSISDN, NotificationChannel.SMS, "welcome-sms");

        assertThat(response.success()).isTrue();
        NotificationRequest saved = response.data();
        assertThat(saved.getTenantId()).isEqualTo(TENANT);
        assertThat(saved.getSubscriberMsisdn()).isEqualTo(MSISDN);
        assertThat(saved.getChannel()).isEqualTo(NotificationChannel.SMS);
        assertThat(saved.getTemplateKey()).isEqualTo("welcome-sms");
        assertThat(saved.getSourceEvent()).isEqualTo("direct-api");
        assertThat(saved.getStatus()).isEqualTo(com.selfcare.notification.domain.NotificationStatus.SENT);
        assertThat(deliveryService.lastDelivered).isSameAs(saved);
    }

    @Test
    void listForSubscriber_returnsRepositoryResultAsIs() {
        List<NotificationRequest> existing = List.of(new NotificationRequest());
        when(repository.findBySubscriberMsisdnOrderByCreatedAtDesc(MSISDN)).thenReturn(existing);

        ApiResponse<List<NotificationRequest>> response = controller.listForSubscriber(MSISDN);

        assertThat(response.data()).isEqualTo(existing);
    }

    private static final class RecordingNotificationDeliveryService extends NotificationDeliveryService {

        private NotificationRequest lastDelivered;

        private RecordingNotificationDeliveryService() {
            super(null, null);
        }

        @Override
        public NotificationRequest deliver(NotificationRequest request) {
            request.setStatus(com.selfcare.notification.domain.NotificationStatus.SENT);
            lastDelivered = request;
            return request;
        }
    }
}
