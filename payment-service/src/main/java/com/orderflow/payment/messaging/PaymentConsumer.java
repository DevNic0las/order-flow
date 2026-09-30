package com.orderflow.payment.messaging;

import com.orderflow.payment.config.RabbitMQConfig;
import com.orderflow.payment.dto.PaymentEventDto;
import com.orderflow.payment.service.PaymentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.AmqpRejectAndDontRequeueException;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
@Slf4j
@RequiredArgsConstructor
public class PaymentConsumer {

  private final PaymentService paymentService;

  @RabbitListener(queues = RabbitMQConfig.PAYMENT_QUEUE)
  public void onPaymentRequest(PaymentEventDto event) {
    log.info("Received payment request: orderId={}, productId={}, quantity={}, eventId={}",
            event.orderId(), event.productId(), event.quantity(), event.eventId());

    try {
      paymentService.processPayment(
              event.eventId(),
              event.orderId(),
              event.productId(),
              event.quantity(),
              event.to()
      );
    } catch (RuntimeException ex) {
      log.warn("Unrecoverable payment processing failure for eventId={}; sending to DLQ", event.eventId(), ex);
      throw new AmqpRejectAndDontRequeueException("Payment processing failed; moving to DLQ", ex);
    }
  }
}
