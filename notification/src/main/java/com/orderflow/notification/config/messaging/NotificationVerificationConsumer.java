package com.orderflow.notification.config.messaging;

import com.orderflow.notification.config.RabbitMQConfig;
import com.orderflow.notification.dto.NotificationEmailVerification;
import com.orderflow.notification.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.AmqpRejectAndDontRequeueException;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
@Slf4j
@RequiredArgsConstructor
public class NotificationVerificationConsumer {

  private final NotificationService notificationService;

  @RabbitListener(queues = RabbitMQConfig.EMAIL_VERIFICATION_QUEUE)
  public void listenVerification(NotificationEmailVerification event) {
    log.info("Received verification event for to={}", maskEmail(event.to()));
    try {
      notificationService.processEmailVerification(event);
    } catch (IllegalArgumentException ex) {
      log.warn("Unrecoverable validation error processing email verification for to={}; moving to DLQ: {}",
              maskEmail(event.to()), ex.getMessage());
      throw new AmqpRejectAndDontRequeueException("Unrecoverable verification error, sending to DLQ", ex);
    } catch (AmqpRejectAndDontRequeueException ex) {
      throw ex;
    } catch (Exception ex) {
      log.error("Failed to process email verification for to={}: {}", maskEmail(event.to()), ex.getMessage());
      throw ex;
    }
  }

  private String maskEmail(String email) {
    if (email == null) return "null";
    int atIndex = email.indexOf('@');
    if (atIndex <= 1) return "***" + (atIndex >= 0 ? email.substring(atIndex) : "");
    return email.charAt(0) + "***" + email.substring(atIndex);
  }
}
