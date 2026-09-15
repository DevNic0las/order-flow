package com.orderflow.notification.dto;

public record NotificationEmailVerification(
      String code,
      String to
) {
}
