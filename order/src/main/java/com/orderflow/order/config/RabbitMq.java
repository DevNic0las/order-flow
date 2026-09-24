package com.orderflow.order.config;


import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMq {
  public static final String ORDER_EXCHANGE       = "order.exchange";
  public static final String ORDER_RESULT_EXCHANGE = "order.result.exchange";
  public static final String DLQ_EXCHANGE         = "dlq.exchange";
  // --- Routing Keys ---
  public static final String RK_INVENTORY   = "rk.inventory";
  // --- Queues ---
  public static final String INVENTORY_QUEUE    = "inventory.queue";
  public static final String ORDER_RESULT_QUEUE = "order.result.queue";
  public static final String ORDER_RESULT_DLQ = "order.result.dlq";
  public static final String INVENTORY_DLQ = "inventory.dlq";
  // ---- Exchanges ----

  @Bean
  public DirectExchange orderExchange() {
    return new DirectExchange(ORDER_EXCHANGE);
  }

  @Bean
  public FanoutExchange orderResultExchange() {
    return new FanoutExchange(ORDER_RESULT_EXCHANGE);
  }

  @Bean
  public DirectExchange dlqExchange() {
    return new DirectExchange(DLQ_EXCHANGE);
  }

  // ---- Queues com DLQ configurada ----

  @Bean
  public Queue inventoryQueue() {
    return QueueBuilder.durable(INVENTORY_QUEUE)
            .withArgument("x-dead-letter-exchange", DLQ_EXCHANGE)
            .withArgument("x-dead-letter-routing-key", INVENTORY_DLQ)
            .build();
  }

  @Bean
  public Queue orderResultQueue() {
    return QueueBuilder.durable(ORDER_RESULT_QUEUE)
            .withArgument("x-dead-letter-exchange", DLQ_EXCHANGE)
            .withArgument("x-dead-letter-routing-key", ORDER_RESULT_DLQ)
            .build();
  }

  // ---- DLQs ----

  @Bean
  public Queue inventoryDlq() {
    return QueueBuilder.durable(INVENTORY_DLQ).build();
  }

  @Bean
  public Queue orderResultDlq() {
    return QueueBuilder.durable(ORDER_RESULT_DLQ).build();
  }

  // ---- Bindings ----

  @Bean
  public Binding inventoryBinding() {
    return BindingBuilder.bind(inventoryQueue())
            .to(orderExchange())
            .with(RK_INVENTORY);
  }

  @Bean
  public Binding orderResultBinding() {
    return BindingBuilder.bind(orderResultQueue())
            .to(orderResultExchange());
  }

  @Bean
  public Binding inventoryDlqBinding() {
    return BindingBuilder.bind(inventoryDlq())
            .to(dlqExchange())
            .with(INVENTORY_DLQ);
  }

  @Bean
  public Binding orderResultDlqBinding() {
    return BindingBuilder.bind(orderResultDlq())
            .to(dlqExchange())
            .with(ORDER_RESULT_DLQ);
  }

  // ---- Serialização JSON ----

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
