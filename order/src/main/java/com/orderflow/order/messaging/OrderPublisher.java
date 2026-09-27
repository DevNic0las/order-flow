package com.orderflow.order.messaging;


import com.orderflow.order.config.RabbitMq;
import com.orderflow.order.dtos.OrderEventDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

@Component
public class OrderPublisher {
  private static final Logger log = LoggerFactory.getLogger(OrderPublisher.class);

  private final RabbitTemplate rabbitTemplate;

  public OrderPublisher(RabbitTemplate rabbitTemplate) {
    this.rabbitTemplate = rabbitTemplate;
  }

  public void publishOrder(OrderEventDto event) {
    log.info("Publishing order event: orderId={} , {}", event.orderId(), event.to());
    rabbitTemplate.convertAndSend(
            RabbitMq.ORDER_EXCHANGE,
            RabbitMq.RK_INVENTORY,
            event
    );
  }
}
