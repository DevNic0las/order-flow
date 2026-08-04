package com.overflow.order.dtos;

public record OrderEventDto(
        Long orderId,
        Long productId,
        Integer quantity
) {
}
