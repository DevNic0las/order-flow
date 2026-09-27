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

  @RabbitListener(queues = RabbitMQConfig.INVENTORY_QUEUE)
  public void onOrderInventoryResult(InventoryEventDto event) {

    log.info("Received inventory result: {}", event);

    try {
      inventoryService.decreaseProductStock(
              event.eventId(),
              event.orderId(),
              event.productId(),
              event.quantity(),
              event.to()
      );
    } catch (OptimisticLockException | ObjectOptimisticLockingFailureException ex) {
      log.warn(
              "Optimistic locking conflict while processing inventory message for orderId={} productId={}: {}",
              event.orderId(),
              event.productId(),
              ex.getMessage()
      );

      throw new AmqpRejectAndDontRequeueException(
              "Optimistic locking conflict, sending to DLQ",
              ex
      );
    }
  }
}
