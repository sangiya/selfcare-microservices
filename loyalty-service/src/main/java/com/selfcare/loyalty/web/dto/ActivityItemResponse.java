package com.selfcare.loyalty.web.dto;

import java.math.BigDecimal;
import java.time.Instant;

public record ActivityItemResponse(
        Long id,
        String actionType,
        String status,
        String channel,
        String counterparty,
        BigDecimal amount,
        String detail,
        Instant createdAt) {
}
