package com.orderflow.notification.config.messaging;

import com.orderflow.notification.dto.NotificationEmailVerification;
import com.orderflow.notification.service.NotificationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.AmqpRejectAndDontRequeueException;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NotificationVerificationConsumerTest {

    @Mock
    private NotificationService notificationService;

    private NotificationVerificationConsumer consumer;

    @BeforeEach
    void setUp() {
        consumer = new NotificationVerificationConsumer(notificationService);
    }

    @Test
    @DisplayName("Should invoke notificationService when verification event is received")
    void shouldInvokeNotificationServiceOnVerificationEvent() {
        NotificationEmailVerification event = new NotificationEmailVerification("123456", "customer@example.com");

        consumer.listenVerification(event);

        verify(notificationService).processEmailVerification(event);
    }

    @Test
    @DisplayName("Should throw AmqpRejectAndDontRequeueException on unrecoverable validation error to route to DLQ")
    void shouldRejectWithoutRequeueOnValidationError() {
        NotificationEmailVerification event = new NotificationEmailVerification(null, "customer@example.com");
        doThrow(new IllegalArgumentException("Code cannot be null")).when(notificationService).processEmailVerification(event);

        assertThrows(AmqpRejectAndDontRequeueException.class, () -> consumer.listenVerification(event));
    }
}
