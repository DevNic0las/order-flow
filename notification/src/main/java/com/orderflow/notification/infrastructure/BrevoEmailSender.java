package com.orderflow.notification.infrastructure;

import com.orderflow.notification.dto.NotificationEventEmailDto;
import com.orderflow.notification.port.EmailSender;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.List;
import java.util.Map;

@Service("emailSender")
@Slf4j
@RequiredArgsConstructor
public class BrevoEmailSender implements EmailSender {

    private final WebClient brevoWebClient;
    private final BrevoProperties brevoProperties;

    @Override
    public void sendMessageEmail(NotificationEventEmailDto event) {
        log.info(
                "Notification email received: to={}, subject={}, body={}",
                event.to(),
                event.subject(),
                event.body()
        );
        if (event.to() == null || event.subject() == null || event.body() == null) {
            throw new IllegalArgumentException("Invalid email notification event");
        }
        var emailBody = Map.of(
                "sender", Map.of(
                        "name", brevoProperties.senderName(),
                        "email", brevoProperties.senderEmail()
                ),
                "to", List.of(
                        Map.of("email", event.to())
                ),
                "subject", event.subject(),
                "textContent", event.body()
        );
        brevoWebClient
                .post()
                .uri("/smtp/email")
                .header("api-key", brevoProperties.apiKey())
                .bodyValue(emailBody)
                .retrieve()
                .bodyToMono(String.class)
                .block();
        log.info("Email sent to {} with subject: {}", event.to(), event.subject());
    }
}