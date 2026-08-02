package com.overflow.inventory.messaging;

import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import com.overflow.inventory.config.RabbitMQConfig;
import com.overflow.inventory.dto.InventoryResultEventDto;

@Component
@Slf4j

public class InventoryConsumer {
  
  @RabbitListener(queues = RabbitMQConfig.INVENTORY_QUEUE)
  public void onOrderInventoryResult(InventoryResultEventDto event) {
    log.info("Received inventory result: {}", event);
  }
}
