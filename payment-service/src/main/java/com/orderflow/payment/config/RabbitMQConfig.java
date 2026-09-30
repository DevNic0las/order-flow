package com.orderflow.payment.config;

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

/**
 * Declara exchanges/filas do payment.
 *
 * <p>Assim como o notification declara a própria fila e o binding à exchange
 * compartilhada da qual consome, o payment declara a {@code payment.queue} e
 * seu binding a {@code payment.exchange} — evita crash-loop caso o payment
 * suba antes do inventory (produtor). As declarações são idempotentes e os
 * argumentos (DLQ) batem com os que o inventory usa.
 */
@Configuration
public class RabbitMQConfig {

  // ---- Exchanges ----
  public static final String PAYMENT_EXCHANGE = "payment.exchange";
  public static final String ORDER_RESULT_EXCHANGE = "order.result.exchange";
  public static final String PAYMENT_COMPENSATION_EXCHANGE = "payment.compensation.exchange";
  public static final String DLQ_EXCHANGE = "dlq.exchange";

  // ---- Routing keys ----
  public static final String RK_PAYMENT = "rk.payment";
  public static final String PAYMENT_DLQ = "payment.dlq";
  public static final String PAYMENT_COMPENSATION_DLQ = "payment.compensation.dlq";

  // ---- Queues ----
  public static final String PAYMENT_QUEUE = "payment.queue";
  public static final String PAYMENT_COMPENSATION_QUEUE = "payment.compensation.queue";
  public static final String PAYMENT_COMPENSATION_DLQ_QUEUE = "payment.compensation.dlq";

  // ---- Exchanges (beans) ----

  @Bean
  public DirectExchange paymentExchange() {
    return new DirectExchange(PAYMENT_EXCHANGE);
  }

  @Bean
  public FanoutExchange orderResultExchange() {
    return new FanoutExchange(ORDER_RESULT_EXCHANGE);
  }

  @Bean
  public FanoutExchange paymentCompensationExchange() {
    return new FanoutExchange(PAYMENT_COMPENSATION_EXCHANGE);
  }

  @Bean
  public DirectExchange dlqExchange() {
    return new DirectExchange(DLQ_EXCHANGE);
  }

  // ---- Consumed queue ----

  @Bean
  public Queue paymentQueue() {
    return QueueBuilder.durable(PAYMENT_QUEUE)
            .withArgument("x-dead-letter-exchange", DLQ_EXCHANGE)
            .withArgument("x-dead-letter-routing-key", PAYMENT_DLQ)
            .build();
  }

  // ---- Compensation queue (published here, also consumed by inventory) ----

  @Bean
  public Queue paymentCompensationQueue() {
    return QueueBuilder.durable(PAYMENT_COMPENSATION_QUEUE)
            .withArgument("x-dead-letter-exchange", DLQ_EXCHANGE)
            .withArgument("x-dead-letter-routing-key", PAYMENT_COMPENSATION_DLQ)
            .build();
  }

  // ---- DLQs ----

  @Bean
  public Queue paymentDlq() {
    return QueueBuilder.durable(PAYMENT_DLQ).build();
  }

  @Bean
  public Queue paymentCompensationDlq() {
    return QueueBuilder.durable(PAYMENT_COMPENSATION_DLQ_QUEUE).build();
  }

  // ---- Bindings ----

  @Bean
  public Binding paymentBinding() {
    return BindingBuilder.bind(paymentQueue())
            .to(paymentExchange())
            .with(RK_PAYMENT);
  }

  @Bean
  public Binding paymentCompensationBinding() {
    return BindingBuilder.bind(paymentCompensationQueue())
            .to(paymentCompensationExchange());
  }

  @Bean
  public Binding paymentDlqBinding() {
    return BindingBuilder.bind(paymentDlq())
            .to(dlqExchange())
            .with(PAYMENT_DLQ);
  }

  @Bean
  public Binding paymentCompensationDlqBinding() {
    return BindingBuilder.bind(paymentCompensationDlq())
            .to(dlqExchange())
            .with(PAYMENT_COMPENSATION_DLQ);
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
