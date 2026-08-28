package com.orderflow.inventory.dto;

public record InventoryEventDto(
        Long orderId,
        Long productId,
        Integer quantity,
        String to
) {
}
