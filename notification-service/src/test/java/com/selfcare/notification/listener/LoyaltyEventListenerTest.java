package com.selfcare.notification.listener;

import static org.assertj.core.api.Assertions.assertThat;

import com.selfcare.notification.domain.NotificationChannel;
import com.selfcare.notification.domain.NotificationRequest;
import com.selfcare.notification.service.NotificationDeliveryService;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * This listener is the one piece of this starter module's business logic that's actually
 * fully implemented end to end (see the class javadoc) -- covers the event-to-entity field
 * mapping for both topics, including the "unknown"/generic-template fallback behavior when an
 * event is missing expected fields.
 */
class LoyaltyEventListenerTest {

    private RecordingNotificationDeliveryService deliveryService;
    private LoyaltyEventListener listener;

    @BeforeEach
    void setUp() {
        deliveryService = new RecordingNotificationDeliveryService();
        listener = new LoyaltyEventListener(deliveryService);
    }

    @Test
    void onPointsEvent_mapsKnownFieldsAndDispatchesPushNotification() {
        Map<String, Object> event = Map.of(
                "tenantId", "acme-telecom",
                "subscriberMsisdn", "94771234567",
                "eventType", "TRANSFER");

        listener.onPointsEvent(event);

        NotificationRequest saved = deliveryService.lastDelivered;
        assertThat(saved.getTenantId()).isEqualTo("acme-telecom");
        assertThat(saved.getSubscriberMsisdn()).isEqualTo("94771234567");
        assertThat(saved.getChannel()).isEqualTo(NotificationChannel.PUSH);
        assertThat(saved.getTemplateKey()).isEqualTo("loyalty-points-transfer");
        assertThat(saved.getSourceEvent()).isEqualTo("loyalty.points.events");
    }

    @Test
    void onPointsEvent_fallsBackToUnknownAndGenericTemplateWhenFieldsMissing() {
        listener.onPointsEvent(Map.of());

        NotificationRequest saved = deliveryService.lastDelivered;
        assertThat(saved.getTenantId()).isEqualTo("unknown");
        assertThat(saved.getSubscriberMsisdn()).isEqualTo("unknown");
        assertThat(saved.getTemplateKey()).isEqualTo("loyalty-points-activity");
    }

    @Test
    void onPartnerRedemptionRequested_dispatchesOpsEmailNotificationRegardlessOfPointsChannel() {
        Map<String, Object> event = Map.of(
                "tenantId", "acme-telecom",
                "subscriberMsisdn", "94771234567");

        listener.onPartnerRedemptionRequested(event);

        NotificationRequest saved = deliveryService.lastDelivered;
        assertThat(saved.getChannel()).isEqualTo(NotificationChannel.EMAIL);
        assertThat(saved.getTemplateKey()).isEqualTo("loyalty-partner-redemption-ops-request");
        assertThat(saved.getSourceEvent()).isEqualTo("loyalty.partner-redemption.requested");
    }

    private static final class RecordingNotificationDeliveryService extends NotificationDeliveryService {

        private NotificationRequest lastDelivered;

        private RecordingNotificationDeliveryService() {
            super(null, null);
        }

        @Override
        public NotificationRequest deliver(NotificationRequest request) {
            lastDelivered = request;
            return request;
        }
    }
}
