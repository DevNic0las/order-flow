package com.orderflow.notification.infrastructure;

import com.orderflow.notification.dto.NotificationEventEmailDto;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.AmqpRejectAndDontRequeueException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BrevoEmailSenderTest {

    @Mock
    private WebClient webClient;

    @Mock
    private WebClient.RequestBodyUriSpec requestBodyUriSpec;

    @Mock
    private WebClient.RequestBodySpec requestBodySpec;

    @Mock
    private WebClient.RequestHeadersSpec requestHeadersSpec;

    @Mock
    private WebClient.ResponseSpec responseSpec;

    @Test
    @DisplayName("Should throw IllegalArgumentException when required fields are null")
    void shouldThrowIllegalArgumentExceptionWhenFieldsAreNull() {
        BrevoProperties properties = new BrevoProperties("key", "test@test.com", "Sender", "https://api.brevo.com");
        BrevoEmailSender sender = new BrevoEmailSender(webClient, properties);

        assertThrows(IllegalArgumentException.class, () ->
                sender.sendMessageEmail(new NotificationEventEmailDto(null, "Subject", "Body")));
        assertThrows(IllegalArgumentException.class, () ->
                sender.sendMessageEmail(new NotificationEventEmailDto("to@test.com", null, "Body")));
        assertThrows(IllegalArgumentException.class, () ->
                sender.sendMessageEmail(new NotificationEventEmailDto("to@test.com", "Subject", null)));
    }

    @Test
    @DisplayName("BrevoProperties should provide fallback defaults when fields are null or blank")
    void shouldProvideFallbackDefaultsInBrevoProperties() {
        BrevoProperties properties = new BrevoProperties(null, null, null, null);

        assertEquals("https://api.brevo.com", properties.baseUrl());
        assertEquals("noreply@orderflow.com", properties.senderEmail());
        assertEquals("Order Flow", properties.senderName());
    }

    @Test
    @DisplayName("Should send email successfully via WebClient")
    void shouldSendEmailSuccessfullyViaWebClient() {
        BrevoProperties properties = new BrevoProperties("test-api-key", null, null, null);
        BrevoEmailSender sender = new BrevoEmailSender(webClient, properties);

        when(webClient.post()).thenReturn(requestBodyUriSpec);
        when(requestBodyUriSpec.uri("/v3/smtp/email")).thenReturn(requestBodySpec);
        when(requestBodySpec.header(eq("api-key"), eq("test-api-key"))).thenReturn(requestBodySpec);
        when(requestBodySpec.bodyValue(any())).thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.bodyToMono(String.class)).thenReturn(Mono.just("{\"messageId\":\"<123>\"}"));

        assertDoesNotThrow(() ->
                sender.sendMessageEmail(new NotificationEventEmailDto("customer@example.com", "Test", "Content")));

        verify(webClient).post();
    }

    @Test
    @DisplayName("Should throw AmqpRejectAndDontRequeueException on 4xx client error from Brevo")
    void shouldThrowAmqpRejectAndDontRequeueOn4xxError() {
        BrevoProperties properties = new BrevoProperties("test-api-key", "sender@test.com", "Sender", "https://api.brevo.com");
        BrevoEmailSender sender = new BrevoEmailSender(webClient, properties);

        when(webClient.post()).thenReturn(requestBodyUriSpec);
        when(requestBodyUriSpec.uri("/v3/smtp/email")).thenReturn(requestBodySpec);
        when(requestBodySpec.header(eq("api-key"), anyString())).thenReturn(requestBodySpec);
        when(requestBodySpec.bodyValue(any())).thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);

        WebClientResponseException badRequestException = WebClientResponseException.create(
                400,
                "Bad Request",
                HttpHeaders.EMPTY,
                "{\"message\":\"invalid email\"}".getBytes(StandardCharsets.UTF_8),
                StandardCharsets.UTF_8
        );

        when(responseSpec.bodyToMono(String.class)).thenThrow(badRequestException);

        assertThrows(AmqpRejectAndDontRequeueException.class, () ->
                sender.sendMessageEmail(new NotificationEventEmailDto("customer@example.com", "Test", "Content")));
    }
}
