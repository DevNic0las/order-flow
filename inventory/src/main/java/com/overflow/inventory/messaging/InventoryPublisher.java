package com.overflow.inventory.messaging;

import com.overflow.inventory.config.RabbitMQConfig;
import com.overflow.inventory.dto.InventoryEventDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;



@Component
@Slf4j
@RequiredArgsConstructor
public class InventoryPublisher {
  private final RabbitTemplate rabbitTemplate;

  public void publishInventoryResult(InventoryEventDto event) {
    log.info("Publishing inventory result with order ID: {}", event.orderId());
    rabbitTemplate.convertAndSend(
            RabbitMQConfig.ORDER_RESULT_EXCHANGE,
            "",
            event
    );
  }

}
