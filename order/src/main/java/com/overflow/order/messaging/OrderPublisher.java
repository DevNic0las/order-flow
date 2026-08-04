package com.overflow.order.messaging;


import com.overflow.order.config.RabbitMq;
import com.overflow.order.dtos.OrderEventDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class OrderPublisher {
  private final RabbitTemplate rabbitTemplate;
  public void publishOrder( OrderEventDto event){
    log.info("Publishing order event: orderId={}", event.orderId());
  rabbitTemplate.convertAndSend(
          RabbitMq.ORDER_EXCHANGE,
          RabbitMq.RK_INVENTORY,
          event
  );
  }

}
