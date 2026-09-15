package com.orderflow.auth.shared.dto;

public record EmailVerificationEventDto(
        String to,
        String code
) {
}
