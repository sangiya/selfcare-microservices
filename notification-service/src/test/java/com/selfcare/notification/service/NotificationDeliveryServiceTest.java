package com.selfcare.notification.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.selfcare.notification.adapter.NotificationProviderAdapter;
import com.selfcare.notification.domain.NotificationChannel;
import com.selfcare.notification.domain.NotificationRequest;
import com.selfcare.notification.domain.NotificationStatus;
import com.selfcare.notification.repository.NotificationRequestRepository;
import com.selfcare.platform.common.adapter.ApiAdapterRegistry;
import com.selfcare.platform.common.tenant.TenantContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class NotificationDeliveryServiceTest {

    @Mock
    private NotificationRequestRepository repository;

    private NotificationDeliveryService service;
    private ApiAdapterRegistry adapterRegistry;

    @BeforeEach
    void setUp() {
        TenantContext.set("acme-telecom");
        adapterRegistry = new ApiAdapterRegistry();
        service = new NotificationDeliveryService(adapterRegistry, repository);
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    @Test
    void deliver_marksRequestSentWhenProviderSucceeds() {
        NotificationRequest request = notificationRequest();
        adapterRegistry.register(NotificationProviderAdapter.class, java.util.List.of(new SuccessAdapter()));
        when(repository.save(request)).thenReturn(request);

        NotificationRequest saved = service.deliver(request);

        assertThat(saved.getStatus()).isEqualTo(NotificationStatus.SENT);
        verify(repository).save(request);
    }

    @Test
    void deliver_marksRequestFailedWhenProviderThrows() {
        NotificationRequest request = notificationRequest();
        adapterRegistry.register(NotificationProviderAdapter.class, java.util.List.of(new FailingAdapter()));
        when(repository.save(request)).thenReturn(request);

        NotificationRequest saved = service.deliver(request);

        assertThat(saved.getStatus()).isEqualTo(NotificationStatus.FAILED);
        verify(repository).save(request);
    }

    private NotificationRequest notificationRequest() {
        NotificationRequest request = new NotificationRequest();
        request.setTenantId(TenantContext.get());
        request.setSubscriberMsisdn("94771234567");
        request.setChannel(NotificationChannel.SMS);
        request.setTemplateKey("welcome-sms");
        request.setSourceEvent("direct-api");
        return request;
    }

    private static final class SuccessAdapter implements NotificationProviderAdapter {

        @Override
        public DeliveryResult send(NotificationRequest request) {
            return new DeliveryResult(NotificationStatus.SENT);
        }

        @Override
        public String tenantId() {
            return ALL_TENANTS;
        }
    }

    private static final class FailingAdapter implements NotificationProviderAdapter {

        @Override
        public DeliveryResult send(NotificationRequest request) {
            throw new IllegalStateException("provider unavailable");
        }

        @Override
        public String tenantId() {
            return ALL_TENANTS;
        }
    }
}
