package com.selfcare.loyalty.event;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.concurrent.CompletableFuture;
import org.junit.jupiter.api.Test;
import org.springframework.kafka.core.KafkaTemplate;

class LoyaltyEventPublisherTest {

    @Test
    void publishPointsTransfer_sendsTheEventToThePointsTopic() {
        @SuppressWarnings("unchecked")
        KafkaTemplate<String, Object> kafkaTemplate = org.mockito.Mockito.mock(KafkaTemplate.class);
        when(kafkaTemplate.send(org.mockito.ArgumentMatchers.anyString(), org.mockito.ArgumentMatchers.anyString(), org.mockito.ArgumentMatchers.any()))
                .thenReturn(new CompletableFuture<>());
        LoyaltyEventPublisher publisher = new LoyaltyEventPublisher(kafkaTemplate);
        PointsTransferEvent event = new PointsTransferEvent(
                "acme-telecom", "TRANSFER", "94771234567", "94779999999", new BigDecimal("25.00"), Instant.now());

        publisher.publishPointsTransfer(event);

        verify(kafkaTemplate).send(LoyaltyEventPublisher.POINTS_EVENTS_TOPIC, "94771234567", event);
    }

    @Test
    void publishPartnerRedemptionRequested_swallowsAsyncPublishFailures() {
        @SuppressWarnings("unchecked")
        KafkaTemplate<String, Object> kafkaTemplate = org.mockito.Mockito.mock(KafkaTemplate.class);
        CompletableFuture<Object> failed = new CompletableFuture<>();
        failed.completeExceptionally(new IllegalStateException("kafka unavailable"));
        when(kafkaTemplate.send(org.mockito.ArgumentMatchers.anyString(), org.mockito.ArgumentMatchers.anyString(), org.mockito.ArgumentMatchers.any()))
                .thenReturn((CompletableFuture) failed);
        LoyaltyEventPublisher publisher = new LoyaltyEventPublisher(kafkaTemplate);
        PartnerRedemptionRequestedEvent event = new PartnerRedemptionRequestedEvent(
                "acme-telecom", "FLYSMILES", "94771234567", "FS-001", new BigDecimal("50.00"), Instant.now());

        publisher.publishPartnerRedemptionRequested(event);

        verify(kafkaTemplate).send(LoyaltyEventPublisher.PARTNER_REDEMPTION_TOPIC, "94771234567", event);
    }
}
