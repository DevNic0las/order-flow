package com.overflow.order.messaging;


import com.overflow.order.config.RabbitMq;
import com.overflow.order.dtos.OrderResultEventDto;
import com.overflow.order.service.OrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
@Slf4j
@RequiredArgsConstructor
public class OrderConsumer {
  private final OrderService orderService;

  @RabbitListener(queues = RabbitMq.ORDER_RESULT_QUEUE)
  public void onOrderResult(OrderResultEventDto event) {
    log.info("Received order result: orderId={}, approved={}",
            event.orderId(), event.approved());

    if (event.approved()) {
      orderService.confirmOrder(event.orderId());
    } else {
      orderService.rejectOrder(event.orderId());
    }
  }
}
