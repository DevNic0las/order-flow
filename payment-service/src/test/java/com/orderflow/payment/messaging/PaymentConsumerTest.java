package com.orderflow.payment.messaging;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.orderflow.payment.dto.PaymentEventDto;
import com.orderflow.payment.service.PaymentService;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.AmqpRejectAndDontRequeueException;

@ExtendWith(MockitoExtension.class)
class PaymentConsumerTest {

  @Mock
  private PaymentService paymentService;

  private PaymentConsumer consumer;

  @BeforeEach
  void setUp() {
    consumer = new PaymentConsumer(paymentService);
  }

  @Test
  @DisplayName("Valid message: delegates to PaymentService with all mapped fields")
  void validMessage_delegatesToService() {
    UUID eventId = UUID.randomUUID();
    PaymentEventDto event = new PaymentEventDto(10L, 7L, 2, "buyer@example.com", eventId);

    consumer.onPaymentRequest(event);

    verify(paymentService).processPayment(
            eventId, 10L, 7L, 2, "buyer@example.com");
  }

  @Test
  @DisplayName("Runtime failure: throws AmqpRejectAndDontRequeueException to route to DLQ")
  void failure_isRejectedWithoutRequeue() {
    UUID eventId = UUID.randomUUID();
    PaymentEventDto event = new PaymentEventDto(10L, 7L, 2, "buyer@example.com", eventId);
    doThrow(new RuntimeException("boom"))
            .when(paymentService).processPayment(any(), any(), any(), any(), any());

    assertThrows(AmqpRejectAndDontRequeueException.class, () -> consumer.onPaymentRequest(event));
  }
}
