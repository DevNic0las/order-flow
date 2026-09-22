package com.orderflow.inventory.dto;

import java.util.UUID;

public record InventoryResultEventDto(
Long orderId,
boolean approved,
String to,
UUID eventId
) {
}
