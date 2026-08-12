package com.selfcare.loyalty.web.dto;

import java.math.BigDecimal;

public record BalanceResponse(BigDecimal currentBalance, BigDecimal redeemableBalance) {
}
