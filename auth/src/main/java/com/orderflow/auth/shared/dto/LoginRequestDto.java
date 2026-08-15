package com.orderflow.auth.shared.dto;

public record LoginRequestDto(
        String email,
        String password
) {
}
