package com.orderflow.inventory.messaging;

import com.orderflow.inventory.config.RabbitMQConfig;
import com.orderflow.inventory.dto.PaymentCompensationEventDto;
import com.orderflow.inventory.service.InventoryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.AmqpRejectAndDontRequeueException;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
@Slf4j
@RequiredArgsConstructor
public class PaymentCompensationConsumer {

  private final InventoryService inventoryService;

  @RabbitListener(queues = RabbitMQConfig.PAYMENT_COMPENSATION_QUEUE)
  public void onPaymentCompensation(PaymentCompensationEventDto event) {
    log.info("Received payment compensation: orderId={}, productId={}, quantity={}, eventId={}",
            event.orderId(), event.productId(), event.quantity(), event.eventId());

    try {
      inventoryService.compensateReservation(
              event.eventId(),
              event.orderId(),
              event.productId(),
              event.quantity()
      );
    } catch (RuntimeException ex) {
      log.warn("Unrecoverable payment compensation failure for eventId={}; sending to DLQ",
              event.eventId(), ex);
      throw new AmqpRejectAndDontRequeueException("Payment compensation failed; moving to DLQ", ex);
    }
  }
}
