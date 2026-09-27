package com.orderflow.web.dto;

public record RegisterRequestDto(
        String email,
        String password,
        String username
) {
}
