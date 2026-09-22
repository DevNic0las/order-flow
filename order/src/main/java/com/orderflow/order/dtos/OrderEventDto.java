package com.orderflow.order.dtos;

import java.util.UUID;

public record OrderEventDto(
        UUID eventId,
        Long orderId,
        Long productId,
        Integer quantity,
        String to
) {
}
