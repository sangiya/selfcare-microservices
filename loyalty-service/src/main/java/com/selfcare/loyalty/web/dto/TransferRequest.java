package com.selfcare.loyalty.web.dto;

import com.selfcare.loyalty.domain.TransferChannel;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

public record TransferRequest(
        @NotBlank(message = "nationalId is required") String nationalId,
        @NotBlank(message = "fromMsisdn is required") String fromMsisdn,
        @NotNull(message = "channel is required") TransferChannel channel,
        @NotBlank(message = "toIdentifier is required") String toIdentifier,
        @NotNull @DecimalMin(value = "0.01", message = "amount must be positive") BigDecimal amount) {
}
