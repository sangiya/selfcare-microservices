package com.selfcare.loyalty.event;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Published instead of the legacy {@code sendEmail(SP_REQUEST_EMAIL, ...)} call when a
 * subscriber requests a FlySmiles/Amex points conversion (StarPointsController::actionTransfer,
 * 'flysmiles'/'amex' branches). notification-service owns turning this into an email/ticket to
 * ops -- loyalty-service no longer needs SMTP configuration or knowledge of how ops requests
 * get routed for any given operator.
 */
public record PartnerRedemptionRequestedEvent(
        String tenantId,
        String channel, // FLYSMILES | AMEX
        String subscriberMsisdn,
        String partnerMemberId,
        BigDecimal amount,
        Instant occurredAt) {
}
