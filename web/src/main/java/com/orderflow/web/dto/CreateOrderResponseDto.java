package com.orderflow.web.dto;

public record CreateOrderResponseDto(
        Long id,
        String customerName,
        Long productId,
        Integer quantity,
        String status
) {
}
