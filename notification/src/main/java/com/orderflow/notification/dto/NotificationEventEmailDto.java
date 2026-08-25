package com.orderflow.notification.dto;

public record NotificationEventEmailDto(
        String to,
        String subject,
        String body
) {
}
