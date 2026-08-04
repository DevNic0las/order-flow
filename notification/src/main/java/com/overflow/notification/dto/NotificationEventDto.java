package com.overflow.notification.dto;

public record NotificationEventDto(
    Long orderId,
    boolean approved
) {
    
}
