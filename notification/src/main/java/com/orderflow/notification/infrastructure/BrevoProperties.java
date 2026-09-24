package com.orderflow.notification.infrastructure;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "brevo")
public record BrevoProperties(
        String apiKey,
        String senderEmail,
        String senderName,
        String baseUrl
) {
    public BrevoProperties {
        if (baseUrl == null || baseUrl.isBlank()) {
            baseUrl = "https://api.brevo.com";
        } else {
            baseUrl = baseUrl.trim();
            while (baseUrl.endsWith("/")) {
                baseUrl = baseUrl.substring(0, baseUrl.length() - 1);
            }
            if (baseUrl.endsWith("/v3")) {
                baseUrl = baseUrl.substring(0, baseUrl.length() - 3);
            }
        }
        if (senderEmail == null || senderEmail.isBlank()) {
            senderEmail = "noreply@orderflow.com";
        }
        if (senderName == null || senderName.isBlank()) {
            senderName = "Order Flow";
        }
    }
}
