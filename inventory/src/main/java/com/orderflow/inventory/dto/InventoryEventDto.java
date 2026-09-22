package com.orderflow.inventory.dto;

import java.util.UUID;

public record InventoryEventDto(
        Long orderId,
        Long productId,
        Integer quantity,
        String to,
        UUID eventId
) {
}
