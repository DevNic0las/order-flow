package com.orderflow.notification.infrastructure;

import com.orderflow.notification.dto.NotificationEventEmailDto;
import com.orderflow.notification.port.EmailSender;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.AmqpRejectAndDontRequeueException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
@Profile("dev")
@Slf4j
@RequiredArgsConstructor
public class SmtpEmailSender implements EmailSender {

    private final JavaMailSender mailSender;

    @Value("${spring.mail.username:noreply@orderflow.com}")
    private String fromEmail;

    @Override
    public void sendMessageEmail(NotificationEventEmailDto event) {
        log.info("Sending email via SMTP/Mailpit: to={}, subject={}", maskEmail(event.to()), event.subject());

        if (event.to() == null || event.subject() == null || event.body() == null) {
            throw new IllegalArgumentException("Invalid email notification event: fields must not be null");
        }

        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromEmail);
            message.setTo(event.to());
            message.setSubject(event.subject());
            message.setText(event.body());
            mailSender.send(message);
            log.info("Email successfully sent via SMTP to {}", maskEmail(event.to()));
        } catch (Exception ex) {
            log.error("Failed to send email via SMTP to {}", maskEmail(event.to()), ex);
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
