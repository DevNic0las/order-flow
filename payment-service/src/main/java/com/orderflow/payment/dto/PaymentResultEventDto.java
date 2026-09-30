package com.orderflow.payment.dto;

import java.util.UUID;

public record PaymentResultEventDto(
        Long orderId,
        boolean approved,
        String to,
        UUID eventId
) {
}
