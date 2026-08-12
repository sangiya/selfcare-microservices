package com.selfcare.loyalty.web.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

public record DonateRequest(
        @NotBlank(message = "nationalId is required") String nationalId,
        @NotBlank(message = "msisdn is required") String msisdn,
        @NotBlank(message = "donationAlias is required") String donationAlias,
        @NotNull @DecimalMin(value = "0.01", message = "amount must be positive") BigDecimal amount) {
}
