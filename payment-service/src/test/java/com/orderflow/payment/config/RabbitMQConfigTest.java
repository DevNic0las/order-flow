package com.orderflow.payment.config;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.FanoutExchange;
import org.springframework.amqp.core.Queue;

class RabbitMQConfigTest {

  private final RabbitMQConfig config = new RabbitMQConfig();

  @Test
  @DisplayName("Consumed queue: correct name, durable, DLQ arguments")
  void paymentQueue_configuredWithDlq() {
    Queue queue = config.paymentQueue();
    assertEquals("payment.queue", queue.getName());
    assertTrue(queue.isDurable());
    assertEquals("dlq.exchange", queue.getArguments().get("x-dead-letter-exchange"));
    assertEquals("payment.dlq", queue.getArguments().get("x-dead-letter-routing-key"));
  }

  @Test
  @DisplayName("Compensation queue: correct name, durable, DLQ arguments")
  void paymentCompensationQueue_configuredWithDlq() {
    Queue queue = config.paymentCompensationQueue();
    assertEquals("payment.compensation.queue", queue.getName());
    assertTrue(queue.isDurable());
    assertEquals("dlq.exchange", queue.getArguments().get("x-dead-letter-exchange"));
    assertEquals("payment.compensation.dlq", queue.getArguments().get("x-dead-letter-routing-key"));
  }

  @Test
  @DisplayName("Exchanges: payment (direct), order.result (fanout), compensation (fanout), dlq (direct)")
  void exchanges_configuredCorrectly() {
    DirectExchange payment = config.paymentExchange();
    assertEquals("payment.exchange", payment.getName());

    FanoutExchange orderResult = config.orderResultExchange();
    assertEquals("order.result.exchange", orderResult.getName());

    FanoutExchange compensation = config.paymentCompensationExchange();
    assertEquals("payment.compensation.exchange", compensation.getName());

    DirectExchange dlq = config.dlqExchange();
    assertEquals("dlq.exchange", dlq.getName());
  }

  @Test
  @DisplayName("Bindings: payment queue ← rk.payment; compensation queue ← fanout")
  void bindings_configuredCorrectly() {
    Binding paymentBinding = config.paymentBinding();
    assertEquals("payment.queue", paymentBinding.getDestination());
    assertEquals("payment.exchange", paymentBinding.getExchange());
    assertEquals("rk.payment", paymentBinding.getRoutingKey());

    Binding compensationBinding = config.paymentCompensationBinding();
    assertEquals("payment.compensation.queue", compensationBinding.getDestination());
    assertEquals("payment.compensation.exchange", compensationBinding.getExchange());
  }

  @Test
  @DisplayName("DLQs: correct names and bindings to dlq.exchange")
  void dlqs_configuredCorrectly() {
    Queue paymentDlq = config.paymentDlq();
    assertEquals("payment.dlq", paymentDlq.getName());

    Queue compensationDlq = config.paymentCompensationDlq();
    assertEquals("payment.compensation.dlq", compensationDlq.getName());

    Binding dlqBinding = config.paymentDlqBinding();
    assertEquals("payment.dlq", dlqBinding.getDestination());
    assertEquals("dlq.exchange", dlqBinding.getExchange());
    assertEquals("payment.dlq", dlqBinding.getRoutingKey());

    Binding compDlqBinding = config.paymentCompensationDlqBinding();
    assertEquals("payment.compensation.dlq", compDlqBinding.getDestination());
    assertEquals("dlq.exchange", compDlqBinding.getExchange());
    assertEquals("payment.compensation.dlq", compDlqBinding.getRoutingKey());
  }
}
