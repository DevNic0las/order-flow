package com.orderflow.web.dto;

public record ResendCodeOutcomeDto(
        boolean resent,
        Long cooldownSeconds
) {
}
