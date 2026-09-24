package com.orderflow.notification.infrastructure;

import com.orderflow.notification.dto.NotificationEventEmailDto;
import com.orderflow.notification.port.EmailSender;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.AmqpRejectAndDontRequeueException;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.time.Duration;
import java.util.List;
import java.util.Map;

@Service("emailSender")
@Profile("!dev")
@Slf4j
@RequiredArgsConstructor
public class BrevoEmailSender implements EmailSender {

    private final WebClient brevoWebClient;
    private final BrevoProperties brevoProperties;

    @Override
    public void sendMessageEmail(NotificationEventEmailDto event) {
        log.info(
                "Sending email via Brevo: to={}, subject={}",
                maskEmail(event.to()),
                event.subject()
        );
        if (event.to() == null || event.subject() == null || event.body() == null) {
            throw new IllegalArgumentException("Invalid email notification event: fields must not be null");
        }

        String senderName = (brevoProperties.senderName() != null && !brevoProperties.senderName().isBlank())
                ? brevoProperties.senderName() : "Order Flow";
        String senderEmail = (brevoProperties.senderEmail() != null && !brevoProperties.senderEmail().isBlank())
                ? brevoProperties.senderEmail() : "noreply@orderflow.com";

        var emailBody = Map.of(
                "sender", Map.of(
                        "name", senderName,
                        "email", senderEmail
                ),
                "to", List.of(
                        Map.of("email", event.to())
                ),
                "subject", event.subject(),
                "textContent", event.body()
        );

        try {
            brevoWebClient
                    .post()
                    .uri("/v3/smtp/email")
                    .header("api-key", brevoProperties.apiKey() != null ? brevoProperties.apiKey() : "")
                    .bodyValue(emailBody)
                    .retrieve()
                    .bodyToMono(String.class)
                    .timeout(Duration.ofSeconds(5))
                    .block();
            log.info("Email successfully sent via Brevo to {}", maskEmail(event.to()));
        } catch (WebClientResponseException ex) {
            if (ex.getStatusCode().is4xxClientError() && ex.getStatusCode().value() != 429) {
                log.error("Permanent client error from Brevo API: status={} body={}",
                        ex.getStatusCode(), ex.getResponseBodyAsString());
                throw new AmqpRejectAndDontRequeueException("Permanent Brevo API client error: " + ex.getStatusCode(), ex);
            }
            log.error("Transient error from Brevo API: status={} body={}",
                    ex.getStatusCode(), ex.getResponseBodyAsString());
            throw ex;
        } catch (Exception ex) {
            log.error("Failed to send email via Brevo to {}", maskEmail(event.to()), ex);
            throw ex;
        }
    }

    private String maskEmail(String email) {
        if (email == null) return "null";
        int atIndex = email.indexOf('@');
        if (atIndex <= 1) return "***" + (atIndex >= 0 ? email.substring(atIndex) : "");
        return email.charAt(0) + "***" + email.substring(atIndex);
    }
}