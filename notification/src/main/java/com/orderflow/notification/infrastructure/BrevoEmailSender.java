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

    private final WebClient.Builder webClientBuilder;
    private final BrevoProperties brevoProperties;

    @Override
    public void sendMessageEmail(NotificationEventEmailDto event) {

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
        webClientBuilder.build()
                .post()
                .uri("/v3/smtp/email")
                .header("api-key", brevoProperties.apiKey())
                .bodyValue(emailBody)
                .retrieve()
                .bodyToMono(String.class)
                .block();
        log.info("Email sent to {} with subject: {}", event.to(), event.subject());
    }
}
