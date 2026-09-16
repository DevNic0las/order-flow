package com.orderflow.auth.shared.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RegisterRequestEmailDto(
        @NotBlank(message = "Token is required")
        String token,

        @NotBlank(message = "Code is required")
        @Size(min = 6, max = 6, message = "Verification code must be 6 digits")
        String code
) {
}
