package com.overflow.notification.config.messaging;

import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;

import com.overflow.notification.config.RabbitMQConfig;
import com.overflow.notification.dto.NotificationEventDto;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
@RequiredArgsConstructor
public class NotificationConsumer {
        private final JavaMailSender mailSender;

    @RabbitListener(queues = RabbitMQConfig.NOTIFICATION_QUEUE)
    public void onOrderResult(NotificationEventDto event) {
        log.info("Received notification event: {}", event);

        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo("customer@example.com");
        message.setSubject("Order " + event.orderId() + " update");
        message.setText(event.approved()
                ? "Your order was approved!"
                : "Your order was rejected due to insufficient stock.");

        mailSender.send(message);
        log.info("Notification email sent for orderId={}", event.orderId());
    }

}
