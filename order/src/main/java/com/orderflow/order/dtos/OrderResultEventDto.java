package com.orderflow.order.dtos;

import java.util.UUID;

public record OrderResultEventDto(
        Long orderId,
        boolean approved,
        UUID eventId
) {
}
