package com.orderflow.auth.shared.dto;

import jakarta.validation.constraints.NotBlank;

public record ResendCodeRequestDto(
        @NotBlank(message = "Token is required")
        String token
) {
}
