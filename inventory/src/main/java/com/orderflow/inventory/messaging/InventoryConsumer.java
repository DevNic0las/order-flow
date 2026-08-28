package com.orderflow.inventory.messaging;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import com.orderflow.inventory.config.RabbitMQConfig;
import com.orderflow.inventory.dto.InventoryEventDto;
import com.orderflow.inventory.service.InventoryService;

@Component
@Slf4j
@RequiredArgsConstructor
public class InventoryConsumer {
  
  private final InventoryService inventoryService;

  @RabbitListener(queues = RabbitMQConfig.INVENTORY_QUEUE)
  public void onOrderInventoryResult(InventoryEventDto event) {
    log.info("Received inventory result: {}", event);
    inventoryService.decreaseProductStock(event.orderId(), event.productId(), event.quantity(),event.to());
  }
}
