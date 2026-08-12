package com.selfcare.loyalty.event;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Published to Kafka on every successful mobile transfer/donation (Doc 1 sec 4.2 -- Kafka as
 * the event backbone, replacing the legacy {@code KafkaOtherServices}/{@code KafkaUtil}
 * calls). Consumed by notification-service (user receipt) and, via CDC-style fan-out, by the
 * AI ingestion pipeline (Doc 3) for analytics.
 */
public record PointsTransferEvent(
        String tenantId,
        String eventType, // TRANSFER | DONATE
        String subscriberMsisdn,
        String counterparty,
        BigDecimal amount,
        Instant occurredAt) {
}
