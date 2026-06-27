package com.overflow.inventory.messaging;

import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class InventoryConsumer {

  @RabbitListener(queues = "inventory-result-queue")
  public void onOrderInventoryResult(Object event) {
    log.info("Received inventory result: {}", event);
  }
}
