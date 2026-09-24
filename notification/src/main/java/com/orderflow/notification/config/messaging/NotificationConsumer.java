package com.orderflow.notification.config.messaging;

import com.orderflow.notification.config.RabbitMQConfig;
import com.orderflow.notification.dto.NotificationEventDto;
import com.orderflow.notification.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.AmqpRejectAndDontRequeueException;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
@Slf4j
@RequiredArgsConstructor
public class NotificationConsumer {

    private final NotificationService notificationService;

    @RabbitListener(queues = RabbitMQConfig.NOTIFICATION_QUEUE)
    public void listen(NotificationEventDto event) {
        log.info("Received notification event: orderId={}, approved={}, to={}, eventId={}",
                event.orderId(), event.approved(), maskEmail(event.to()), event.eventId());
        try {
            notificationService.processNotification(event);
        } catch (IllegalArgumentException ex) {
            log.warn("Unrecoverable validation error processing notification for orderId={}; moving to DLQ: {}",
                    event.orderId(), ex.getMessage());
            throw new AmqpRejectAndDontRequeueException("Unrecoverable notification error, sending to DLQ", ex);
        } catch (AmqpRejectAndDontRequeueException ex) {
            throw ex;
        } catch (Exception ex) {
            log.error("Failed to process notification for orderId={}: {}", event.orderId(), ex.getMessage());
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
