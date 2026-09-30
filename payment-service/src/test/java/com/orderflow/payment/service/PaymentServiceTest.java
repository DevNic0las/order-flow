package com.orderflow.payment.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.orderflow.payment.dto.PaymentCompensationEventDto;
import com.orderflow.payment.dto.PaymentResultEventDto;
import com.orderflow.payment.messaging.PaymentPublisher;
import com.orderflow.payment.repository.ProcessedPaymentEventRepository;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PaymentServiceTest {

  @Mock
  private PaymentPublisher paymentPublisher;

  @Mock
  private ProcessedPaymentEventRepository processedPaymentEventRepository;

  private PaymentService paymentService;

  @BeforeEach
  void setUp() {
    paymentService = new PaymentService(paymentPublisher, processedPaymentEventRepository);
  }

  // ────────────────────────── aprovado ──────────────────────────

  @Test
  @DisplayName("Approved event: publishes PaymentResultEventDto with approved=true and no compensation")
  void approvedEvent_publishesApprovedResult_andNoCompensation() {
    UUID eventId = UUID.randomUUID();
    when(processedPaymentEventRepository.insertIfNotExists(eventId)).thenReturn(1);

    paymentService.processPayment(eventId, 10L, 7L, 2, "buyer@example.com");

    ArgumentCaptor<PaymentResultEventDto> resultCaptor = ArgumentCaptor.forClass(PaymentResultEventDto.class);
    verify(paymentPublisher, times(1)).publishPaymentResult(resultCaptor.capture());

    PaymentResultEventDto result = resultCaptor.getValue();
    assertEquals(10L, result.orderId());
    assertTrue(result.approved());
    assertEquals("buyer@example.com", result.to());
    assertEquals(eventId, result.eventId());

    verify(paymentPublisher, never()).publishPaymentCompensation(any());
  }

  // ────────────────────────── recusado ──────────────────────────

  @Test
  @DisplayName("Refused event: publishes rejected result AND compensation with the same eventId")
  void refusedEvent_publishesRejectedResult_andCompensation() {
    UUID eventId = UUID.randomUUID();
    when(processedPaymentEventRepository.insertIfNotExists(eventId)).thenReturn(1);

    PaymentService refusingService = new PaymentService(paymentPublisher, processedPaymentEventRepository) {
      @Override
      protected boolean authorize(Long orderId, Long productId, Integer quantity) {
        return false;
      }
    };

    refusingService.processPayment(eventId, 10L, 7L, 2, "buyer@example.com");

    ArgumentCaptor<PaymentResultEventDto> resultCaptor = ArgumentCaptor.forClass(PaymentResultEventDto.class);
    verify(paymentPublisher, times(1)).publishPaymentResult(resultCaptor.capture());
    PaymentResultEventDto result = resultCaptor.getValue();
    assertEquals(10L, result.orderId());
    assertFalse(result.approved());
    assertEquals("buyer@example.com", result.to());
    assertEquals(eventId, result.eventId());

    ArgumentCaptor<PaymentCompensationEventDto> compCaptor = ArgumentCaptor.forClass(PaymentCompensationEventDto.class);
    verify(paymentPublisher, times(1)).publishPaymentCompensation(compCaptor.capture());
    PaymentCompensationEventDto compensation = compCaptor.getValue();
    assertEquals(10L, compensation.orderId());
    assertEquals(7L, compensation.productId());
    assertEquals(2, compensation.quantity());
    assertEquals(eventId, compensation.eventId());
  }

  // ─────────────────── eventId preservado / sem UUID novo ───────────────────

  @Test
  @DisplayName("Preserves the received eventId across result and compensation; generates no new UUID")
  void preservesReceivedEventId_onAllPublishedEvents() {
    UUID eventId = UUID.randomUUID();
    when(processedPaymentEventRepository.insertIfNotExists(eventId)).thenReturn(1);

    PaymentService refusingService = new PaymentService(paymentPublisher, processedPaymentEventRepository) {
      @Override
      protected boolean authorize(Long orderId, Long productId, Integer quantity) {
        return false;
      }
    };

    refusingService.processPayment(eventId, 10L, 7L, 2, "buyer@example.com");

    ArgumentCaptor<PaymentResultEventDto> resultCaptor = ArgumentCaptor.forClass(PaymentResultEventDto.class);
    ArgumentCaptor<PaymentCompensationEventDto> compCaptor = ArgumentCaptor.forClass(PaymentCompensationEventDto.class);
    verify(paymentPublisher).publishPaymentResult(resultCaptor.capture());
    verify(paymentPublisher).publishPaymentCompensation(compCaptor.capture());

    assertEquals(eventId, resultCaptor.getValue().eventId(),
            "Result must carry the received eventId, not a new one");
    assertEquals(eventId, compCaptor.getValue().eventId(),
            "Compensation must carry the received eventId, not a new one");
  }

  // ────────────────────────── idempotência ──────────────────────────

  @Test
  @DisplayName("Duplicate event: insertIfNotExists returns 0 → nothing is published and no re-processing")
  void duplicateEvent_isIgnored() {
    UUID eventId = UUID.randomUUID();
    when(processedPaymentEventRepository.insertIfNotExists(eventId)).thenReturn(0);

    paymentService.processPayment(eventId, 10L, 7L, 2, "buyer@example.com");

    verify(paymentPublisher, never()).publishPaymentResult(any());
    verify(paymentPublisher, never()).publishPaymentCompensation(any());
  }

  @Test
  @DisplayName("Idempotency uses insertIfNotExists (once, with the received eventId)")
  void idempotency_usesInsertIfNotExists() {
    UUID eventId = UUID.randomUUID();
    when(processedPaymentEventRepository.insertIfNotExists(eventId)).thenReturn(1);

    paymentService.processPayment(eventId, 10L, 7L, 2, "buyer@example.com");

    verify(processedPaymentEventRepository, times(1)).insertIfNotExists(eventId);
    verify(processedPaymentEventRepository, never()).save(any());
    verify(processedPaymentEventRepository, never()).saveAndFlush(any());
  }
}
