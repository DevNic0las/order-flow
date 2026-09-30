package com.orderflow.payment.messaging;

import com.orderflow.payment.config.RabbitMQConfig;
import com.orderflow.payment.dto.PaymentCompensationEventDto;
import com.orderflow.payment.dto.PaymentResultEventDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

@Component
@Slf4j
@RequiredArgsConstructor
public class PaymentPublisher {

  private final RabbitTemplate rabbitTemplate;

  public void publishPaymentResult(PaymentResultEventDto event) {
    log.info("Publishing payment result: orderId={}, approved={}", event.orderId(), event.approved());
    rabbitTemplate.convertAndSend(
            RabbitMQConfig.ORDER_RESULT_EXCHANGE,
            "",
            event
    );
  }

  public void publishPaymentCompensation(PaymentCompensationEventDto event) {
    log.info("Publishing payment compensation: orderId={}, productId={}", event.orderId(), event.productId());
    rabbitTemplate.convertAndSend(
            RabbitMQConfig.PAYMENT_COMPENSATION_EXCHANGE,
            "",
            event
    );
  }
}
