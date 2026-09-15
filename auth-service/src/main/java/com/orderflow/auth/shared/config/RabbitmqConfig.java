package com.orderflow.auth.shared.config;


import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitmqConfig {

  public static final String NOTIFICATION_EXCHANGE =
          "notification.exchange";

  public static final String RK_EMAIL_VERIFICATION =
          "rk.email.verification";

  @Bean
  public MessageConverter messageConverter() {
    return new Jackson2JsonMessageConverter();
  }

  @Bean
  public RabbitTemplate rabbitTemplate(
          ConnectionFactory connectionFactory) {

    RabbitTemplate template =
            new RabbitTemplate(connectionFactory);

    template.setMessageConverter(messageConverter());

    return template;
  }
}
