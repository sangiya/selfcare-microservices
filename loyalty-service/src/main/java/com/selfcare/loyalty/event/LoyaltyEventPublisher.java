package com.selfcare.loyalty.event;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
public class LoyaltyEventPublisher {

    private static final Logger log = LoggerFactory.getLogger(LoyaltyEventPublisher.class);

    public static final String POINTS_EVENTS_TOPIC = "loyalty.points.events";
    public static final String PARTNER_REDEMPTION_TOPIC = "loyalty.partner-redemption.requested";

    private final KafkaTemplate<String, Object> kafkaTemplate;

    public LoyaltyEventPublisher(KafkaTemplate<String, Object> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    public void publishPointsTransfer(PointsTransferEvent event) {
        publish(POINTS_EVENTS_TOPIC, event.subscriberMsisdn(), event);
    }

    public void publishPartnerRedemptionRequested(PartnerRedemptionRequestedEvent event) {
        publish(PARTNER_REDEMPTION_TOPIC, event.subscriberMsisdn(), event);
    }

    private void publish(String topic, String key, Object payload) {
        kafkaTemplate.send(topic, key, payload).whenComplete((result, ex) -> {
            if (ex != null) {
                // A Kafka publish failure must never fail the user-facing action that already
                // succeeded against the loyalty core -- log loudly (feeds Sentry/alerting via
                // Doc 2 sec 4) and move on; the audit trail row is the durable record either way.
                log.error("Failed to publish event to topic {}: {}", topic, payload, ex);
            }
        });
    }
}
