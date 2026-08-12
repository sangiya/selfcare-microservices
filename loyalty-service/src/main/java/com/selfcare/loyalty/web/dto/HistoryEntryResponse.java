package com.selfcare.loyalty.web.dto;

import java.math.BigDecimal;

public record HistoryEntryResponse(
        String transactionSerial, String transactionType, String merchant, BigDecimal amount, String occurredAt) {
}
