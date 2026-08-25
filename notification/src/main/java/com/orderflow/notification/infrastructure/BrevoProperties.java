package com.orderflow.notification.infrastructure;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "brevo")
public record BrevoProperties(
        String apiKey,
        String senderEmail,
        String senderName
) {
}
