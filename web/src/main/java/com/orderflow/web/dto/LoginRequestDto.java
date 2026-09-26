package com.orderflow.web.dto;

public record LoginRequestDto(
        String email,
        String password
) {
}
