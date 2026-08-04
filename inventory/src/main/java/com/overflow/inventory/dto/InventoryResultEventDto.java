package com.overflow.inventory.dto;

public record InventoryResultEventDto(
Long orderId,
boolean approved
) {
}
