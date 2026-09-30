package com.orderflow.payment.dto;

import java.util.UUID;

public record PaymentCompensationEventDto(
        Long orderId,
        Long productId,
        Integer quantity,
        UUID eventId
) {
}
