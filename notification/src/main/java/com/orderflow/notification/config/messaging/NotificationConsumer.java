package com.orderflow.notification.config.messaging;

import com.orderflow.notification.port.EmailSender;
import com.orderflow.notification.config.RabbitMQConfig;
import com.orderflow.notification.dto.NotificationEventDto;
import com.orderflow.notification.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
@Slf4j
@RequiredArgsConstructor
public class NotificationConsumer {
    private final NotificationService sendMessage;
    @RabbitListener(queues = RabbitMQConfig.NOTIFICATION_QUEUE)
    public void listen(NotificationEventDto event) {
        log.info("Received notification event: {}", event);
        sendMessage.processNotification(event);

    }
}
