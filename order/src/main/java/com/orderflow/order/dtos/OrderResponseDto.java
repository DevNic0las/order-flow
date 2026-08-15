package com.orderflow.order.dtos;

import java.time.LocalDateTime;

public record OrderResponseDto(
        Long id,
        String customerName,
        Long productId,
        Integer quantity,
        String status,
        LocalDateTime createdAt
) {
}
