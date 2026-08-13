package com.selfcare.notification.listener;

import com.selfcare.notification.domain.NotificationChannel;
import com.selfcare.notification.domain.NotificationRequest;
import com.selfcare.notification.service.NotificationDeliveryService;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/**
 * Live example of the event-driven pattern described in Doc 1 sec 4.2: loyalty-service
 * publishes {@code loyalty.points.events} / {@code loyalty.partner-redemption.requested}
 * (see {@code LoyaltyEventPublisher}) without knowing or caring who consumes them; this
 * listener is one consumer, turning each event into a notification request and handing it to the
 * provider-adapter-backed delivery service. Operators can override the default adapter per tenant.
 */
@Component
public class LoyaltyEventListener {

    private static final Logger log = LoggerFactory.getLogger(LoyaltyEventListener.class);

    private final NotificationDeliveryService deliveryService;

    public LoyaltyEventListener(NotificationDeliveryService deliveryService) {
        this.deliveryService = deliveryService;
    }

    @KafkaListener(topics = "loyalty.points.events", groupId = "${spring.kafka.consumer.group-id}")
    public void onPointsEvent(Map<String, Object> event) {
        log.info("Received loyalty.points.events: {}", event);
        NotificationRequest request = new NotificationRequest();
        request.setTenantId(String.valueOf(event.getOrDefault("tenantId", "unknown")));
        request.setSubscriberMsisdn(String.valueOf(event.getOrDefault("subscriberMsisdn", "unknown")));
        request.setChannel(NotificationChannel.PUSH);
        request.setTemplateKey("loyalty-points-" + String.valueOf(event.getOrDefault("eventType", "activity")).toLowerCase());
        request.setPayloadJson(event.toString());
        request.setSourceEvent("loyalty.points.events");
        deliveryService.deliver(request);
    }

    @KafkaListener(topics = "loyalty.partner-redemption.requested", groupId = "${spring.kafka.consumer.group-id}")
    public void onPartnerRedemptionRequested(Map<String, Object> event) {
        log.info("Received loyalty.partner-redemption.requested: {}", event);
        NotificationRequest request = new NotificationRequest();
        request.setTenantId(String.valueOf(event.getOrDefault("tenantId", "unknown")));
        request.setSubscriberMsisdn(String.valueOf(event.getOrDefault("subscriberMsisdn", "unknown")));
        // Legacy behavior emailed an internal ops mailbox (SP_REQUEST_EMAIL) -- replaced by an
        // EMAIL notification to ops via the (TODO) provider adapter, instead of the loyalty
        // service owning SMTP details.
        request.setChannel(NotificationChannel.EMAIL);
        request.setTemplateKey("loyalty-partner-redemption-ops-request");
        request.setPayloadJson(event.toString());
        request.setSourceEvent("loyalty.partner-redemption.requested");
        deliveryService.deliver(request);
    }
}
