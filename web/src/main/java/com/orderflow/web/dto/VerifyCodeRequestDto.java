package com.orderflow.web.dto;

public record VerifyCodeRequestDto(
        String token,
        String code
) {
}
