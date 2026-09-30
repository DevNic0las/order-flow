package com.orderflow.web.dto;

public record OrderCreatedDto(
        Long orderId,
        String status
) {
}
