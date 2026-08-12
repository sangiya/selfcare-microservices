package com.selfcare.loyalty.web.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record RegisterRequest(
        @NotBlank(message = "nationalId is required") String nationalId,
        @NotBlank(message = "msisdn is required") String msisdn,
        @NotBlank(message = "idType is required") String idType,
        @NotBlank(message = "idNumber is required") String idNumber,
        String name,
        String address,
        @Email(message = "email must be valid") String email) {
}
