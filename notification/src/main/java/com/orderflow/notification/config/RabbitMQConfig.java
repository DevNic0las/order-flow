package com.orderflow.notification.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

  public static final String NOTIFICATION_QUEUE = "notification.queue";
  public static final String EMAIL_VERIFICATION_QUEUE = "email.verification.queue";
  public static final String ORDER_RESULT_EXCHANGE = "order.result.exchange";
  public static final String NOTIFICATION_EXCHANGE = "notification.exchange";
  public static final String DLQ_EXCHANGE = "dlq.exchange";
  public static final String RK_EMAIL_VERIFICATION = "rk.email.verification";
  public static final String NOTIFICATION_DLQ = "notification.dlq";
  public static final String EMAIL_VERIFICATION_DLQ = "email.verification.dlq";

  // ---- Exchanges ----

  @Bean
  public FanoutExchange orderResultExchange() {
    return new FanoutExchange(ORDER_RESULT_EXCHANGE);
  }

  @Bean
  public DirectExchange notificationExchange() {
    return new DirectExchange(NOTIFICATION_EXCHANGE);
  }

  @Bean
  public DirectExchange dlqExchange() {
    return new DirectExchange(DLQ_EXCHANGE);
  }

  // ---- Queues with DLQ ----

  @Bean
  public Queue notificationQueue() {
    return QueueBuilder.durable(NOTIFICATION_QUEUE)
            .withArgument("x-dead-letter-exchange", DLQ_EXCHANGE)
            .withArgument("x-dead-letter-routing-key", NOTIFICATION_DLQ)
            .build();
  }

  @Bean
  public Queue emailVerificationQueue() {
    return QueueBuilder.durable(EMAIL_VERIFICATION_QUEUE)
            .withArgument("x-dead-letter-exchange", DLQ_EXCHANGE)
            .withArgument("x-dead-letter-routing-key", EMAIL_VERIFICATION_DLQ)
            .build();
  }

  @Bean
  public Queue notificationDlq() {
    return QueueBuilder.durable(NOTIFICATION_DLQ).build();
  }

  @Bean
  public Queue emailVerificationDlq() {
    return QueueBuilder.durable(EMAIL_VERIFICATION_DLQ).build();
  }

  // ---- Bindings ----

  @Bean
  public Binding notificationBinding() {
    return BindingBuilder.bind(notificationQueue())
            .to(orderResultExchange());
  }

  @Bean
  public Binding emailVerificationBinding() {
    return BindingBuilder.bind(emailVerificationQueue())
            .to(notificationExchange())
            .with(RK_EMAIL_VERIFICATION);
  }

  @Bean
  public Binding notificationDlqBinding() {
    return BindingBuilder.bind(notificationDlq())
            .to(dlqExchange())
            .with(NOTIFICATION_DLQ);
  }

  @Bean
  public Binding emailVerificationDlqBinding() {
    return BindingBuilder.bind(emailVerificationDlq())
            .to(dlqExchange())
            .with(EMAIL_VERIFICATION_DLQ);
  }

  // ---- Converter & Template ----

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
