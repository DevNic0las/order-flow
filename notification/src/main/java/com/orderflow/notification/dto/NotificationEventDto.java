package com.orderflow.notification.dto;

public record NotificationEventDto(
    Long orderId,
    boolean approved
) {
    
}
