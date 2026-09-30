package com.orderflow.web.dto;

public record CreateOrderRequestDto(
        Long productId,
        Integer quantity
) {
}
