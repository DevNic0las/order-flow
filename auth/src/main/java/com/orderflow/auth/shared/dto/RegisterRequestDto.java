package com.orderflow.auth.shared.dto;

public record RegisterRequestDto(
        String email,
        String password
) {
}
