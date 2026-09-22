package com.orderflow.inventory.messaging;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.AmqpRejectAndDontRequeueException;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.support.converter.MessageConversionException;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.stereotype.Component;

import com.orderflow.inventory.config.RabbitMQConfig;
import com.orderflow.inventory.dto.InventoryEventDto;
import com.orderflow.inventory.service.InventoryService;
import jakarta.persistence.OptimisticLockException;

@Component
@Slf4j
@RequiredArgsConstructor
public class InventoryConsumer {

  private final InventoryService inventoryService;
  private final MessageConverter messageConverter;

  @RabbitListener(queues = RabbitMQConfig.INVENTORY_QUEUE)
  public void onOrderInventoryResult(Message message) {
    final InventoryEventDto event;

    try {
      event = (InventoryEventDto) messageConverter.fromMessage(message);
    } catch (MessageConversionException ex) {
      log.warn("Malformed inventory message received; rejecting without requeue. messageId={} payload={}",
          message.getMessageProperties().getMessageId(), new String(message.getBody()));
      throw new AmqpRejectAndDontRequeueException("Malformed inventory message, sending to DLQ", ex);
    }

    log.info("Received inventory result: {}", event);
    try {
      inventoryService.decreaseProductStock(event.eventId(), event.orderId(), event.productId(), event.quantity(), event.to());
    } catch (OptimisticLockException | ObjectOptimisticLockingFailureException ex) {
      log.warn("Optimistic locking conflict while processing inventory message for orderId={} productId={}: {}",
          event.orderId(), event.productId(), ex.getMessage());
      throw new AmqpRejectAndDontRequeueException("Optimistic locking conflict, sending to DLQ", ex);
    }
  }
}
