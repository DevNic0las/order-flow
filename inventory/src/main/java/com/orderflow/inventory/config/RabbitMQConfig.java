package com.orderflow.inventory.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.FanoutExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

  // Fila consumida pelo inventory (evento de pedido criado vindo do order).
  public static final String INVENTORY_QUEUE = "inventory.queue";
  public static final String INVENTORY_DLQ = "inventory.dlq";
  // Exchange do order (direta) à qual a inventory.queue é vinculada.
  public static final String ORDER_EXCHANGE = "order.exchange";
  public static final String RK_INVENTORY = "rk.inventory";

  public static final String ORDER_RESULT_EXCHANGE = "order.result.exchange";

  // Payment exchange (novo): inventory publica pedido de pagamento aqui.
  // A fila payment.queue é consumida pelo payment-service, que a declara.
  public static final String PAYMENT_EXCHANGE = "payment.exchange";
  public static final String RK_PAYMENT = "rk.payment";

  // Compensação: payment devolve estoque; inventory consome.
  public static final String PAYMENT_COMPENSATION_EXCHANGE = "payment.compensation.exchange";
  public static final String PAYMENT_COMPENSATION_QUEUE = "payment.compensation.queue";

  public static final String DLQ_EXCHANGE = "dlq.exchange";
  public static final String PAYMENT_COMPENSATION_DLQ = "payment.compensation.dlq";

  @Bean
  public DirectExchange orderExchange() {
    return new DirectExchange(ORDER_EXCHANGE);
  }

  @Bean
  public Queue inventoryQueue() {
    return QueueBuilder.durable(INVENTORY_QUEUE)
            .withArgument("x-dead-letter-exchange", DLQ_EXCHANGE)
            .withArgument("x-dead-letter-routing-key", INVENTORY_DLQ)
            .build();
  }

  @Bean
  public Queue inventoryDlq() {
    return QueueBuilder.durable(INVENTORY_DLQ).build();
  }

  @Bean
  public Binding inventoryBinding() {
    return BindingBuilder.bind(inventoryQueue())
            .to(orderExchange())
            .with(RK_INVENTORY);
  }

  @Bean
  public Binding inventoryDlqBinding() {
    return BindingBuilder.bind(inventoryDlq())
            .to(dlqExchange())
            .with(INVENTORY_DLQ);
  }

  @Bean
  public DirectExchange paymentExchange() {
    return new DirectExchange(PAYMENT_EXCHANGE);
  }

  @Bean
  public FanoutExchange paymentCompensationExchange() {
    return new FanoutExchange(PAYMENT_COMPENSATION_EXCHANGE);
  }

  @Bean
  public DirectExchange dlqExchange() {
    return new DirectExchange(DLQ_EXCHANGE);
  }

  @Bean
  public Queue paymentCompensationQueue() {
    return QueueBuilder.durable(PAYMENT_COMPENSATION_QUEUE)
            .withArgument("x-dead-letter-exchange", DLQ_EXCHANGE)
            .withArgument("x-dead-letter-routing-key", PAYMENT_COMPENSATION_DLQ)
            .build();
  }

  @Bean
  public Queue paymentCompensationDlq() {
    return QueueBuilder.durable(PAYMENT_COMPENSATION_DLQ).build();
  }

  @Bean
  public Binding paymentCompensationBinding() {
    return BindingBuilder.bind(paymentCompensationQueue())
            .to(paymentCompensationExchange());
  }

  @Bean
  public Binding paymentCompensationDlqBinding() {
    return BindingBuilder.bind(paymentCompensationDlq())
            .to(dlqExchange())
            .with(PAYMENT_COMPENSATION_DLQ);
  }

  @Bean
  public MessageConverter messageConverter() {
    return new Jackson2JsonMessageConverter();
  }

  @Bean
  public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory) {
    RabbitTemplate template = new RabbitTemplate(connectionFactory);
    template.setMessageConverter(messageConverter());
    return template;
  }
}
