package com.overflow.inventory.messaging;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import com.overflow.inventory.config.RabbitMQConfig;
import com.overflow.inventory.dto.InventoryEventDto;
import com.overflow.inventory.service.InventoryService;

@Component
@Slf4j
@RequiredArgsConstructor
public class InventoryConsumer {
  
  private final InventoryService inventoryService;

  @RabbitListener(queues = RabbitMQConfig.INVENTORY_QUEUE)
  public void onOrderInventoryResult(InventoryEventDto event) {
    log.info("Received inventory result: {}", event);
    inventoryService.decreaseProductStock(event.orderId(), event.productId(), event.quantity());
  }
}
