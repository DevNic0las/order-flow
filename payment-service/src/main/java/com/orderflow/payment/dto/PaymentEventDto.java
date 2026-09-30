package com.orderflow.payment.dto;

import java.util.UUID;

public record PaymentEventDto(
        Long orderId,
        Long productId,
        Integer quantity,
        String to,
        UUID eventId
) {
}
