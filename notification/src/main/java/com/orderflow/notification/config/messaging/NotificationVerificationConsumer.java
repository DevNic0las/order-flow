package com.orderflow.notification.config.messaging;


import com.orderflow.notification.config.RabbitMQConfig;
import com.orderflow.notification.dto.NotificationEmailVerification;
import com.orderflow.notification.dto.NotificationEventDto;
import com.orderflow.notification.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;

@Component
@Slf4j
@RequiredArgsConstructor
public class NotificationVerificationConsumer {
  private final NotificationService sendMessage;

  @RabbitListener(queues = RabbitMQConfig.EMAIL_VERIFICATION_QUEUE)
  public void listenVerification(NotificationEmailVerification event) {
    log.info("Received verification event: {}", event);
    sendMessage.processEmailVerification(event);
  }


}
