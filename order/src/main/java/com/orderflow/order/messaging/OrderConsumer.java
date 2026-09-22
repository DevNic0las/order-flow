package com.orderflow.order.messaging;

import com.orderflow.order.config.RabbitMq;
import com.orderflow.order.dtos.OrderResultEventDto;
import com.orderflow.order.repository.ProcessedOrderResultEventRepository;
import com.orderflow.order.service.OrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.AmqpRejectAndDontRequeueException;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
@Slf4j
@RequiredArgsConstructor
public class OrderConsumer {
  private final OrderService orderService;
  private final ProcessedOrderResultEventRepository processedOrderResultEventRepository;

  @RabbitListener(queues = RabbitMq.ORDER_RESULT_QUEUE)
  public void onOrderResult(OrderResultEventDto event) {
    try {
      log.info("Received order result: orderId={}, approved={}, eventId={}",
              event.orderId(), event.approved(), event.eventId());

      int inserted = processedOrderResultEventRepository.insertIfNotExists(event.eventId());
      if (inserted == 0) {
        log.info("Event already processed, ignoring. eventId={}", event.eventId());
        return;
      }

      if (event.approved()) {
        orderService.confirmOrder(event.orderId());
      } else {
        orderService.rejectOrder(event.orderId());
      }
    } catch (RuntimeException ex) {
      log.warn("Unrecoverable order result processing failure for eventId={}; sending to DLQ", event.eventId(), ex);
      throw new AmqpRejectAndDontRequeueException("Order result processing failed; moving to DLQ", ex);
    }
  }
}
