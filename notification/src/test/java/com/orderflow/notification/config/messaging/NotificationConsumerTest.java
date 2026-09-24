package com.orderflow.notification.config.messaging;

import com.orderflow.notification.dto.NotificationEventDto;
import com.orderflow.notification.service.NotificationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.AmqpRejectAndDontRequeueException;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NotificationConsumerTest {

    @Mock
    private NotificationService notificationService;

    private NotificationConsumer consumer;

    @BeforeEach
    void setUp() {
        consumer = new NotificationConsumer(notificationService);
    }

    @Test
    @DisplayName("Should invoke notificationService when event is received")
    void shouldInvokeNotificationServiceOnEvent() {
        NotificationEventDto event = new NotificationEventDto(1L, true, "customer@example.com", UUID.randomUUID());

        consumer.listen(event);

        verify(notificationService).processNotification(event);
    }

    @Test
    @DisplayName("Should throw AmqpRejectAndDontRequeueException on unrecoverable validation error to route to DLQ")
    void shouldRejectWithoutRequeueOnValidationError() {
        NotificationEventDto event = new NotificationEventDto(1L, true, null, UUID.randomUUID());
        doThrow(new IllegalArgumentException("Invalid email")).when(notificationService).processNotification(event);

        assertThrows(AmqpRejectAndDontRequeueException.class, () -> consumer.listen(event));
    }
}
