package com.orderflow.inventory.dto;

public record InventoryResultEventDto(
Long orderId,
boolean approved
) {
}
