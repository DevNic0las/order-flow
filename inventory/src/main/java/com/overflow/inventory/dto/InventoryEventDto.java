package com.overflow.inventory.dto;

public record InventoryEventDto(
        Long orderId,
        Long productId,
        Integer quantity
) {
}
