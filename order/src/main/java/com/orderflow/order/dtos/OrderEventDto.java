package com.orderflow.order.dtos;

public record OrderEventDto(
        Long orderId,
        Long productId,
        Integer quantity
) {
}
