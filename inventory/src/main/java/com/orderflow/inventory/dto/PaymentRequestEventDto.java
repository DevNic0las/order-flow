package com.orderflow.inventory.dto;

import java.util.UUID;

public record PaymentRequestEventDto(
        Long orderId,
        Long productId,
        Integer quantity,
        String to,
        UUID eventId
) {
}
