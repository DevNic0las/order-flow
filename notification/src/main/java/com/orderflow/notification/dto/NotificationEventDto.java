package com.orderflow.notification.dto;

import java.util.UUID;

public record NotificationEventDto(
    Long orderId,
    boolean approved,
    String to,
    UUID eventId
) {
}
