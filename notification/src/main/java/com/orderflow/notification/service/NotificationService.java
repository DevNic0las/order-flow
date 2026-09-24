package com.orderflow.notification.service;

import com.orderflow.notification.dto.NotificationEmailVerification;
import com.orderflow.notification.dto.NotificationEventDto;
import com.orderflow.notification.dto.NotificationEventEmailDto;
import com.orderflow.notification.port.EmailSender;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationService {

  private final EmailSender emailSender;

  public void processNotification(NotificationEventDto event) {
    if (event.to() == null || event.to().isBlank() || event.orderId() == null) {
      throw new IllegalArgumentException("Invalid notification event: to/orderId must not be null");
    }

    String subject = event.approved() ? "Pedido aprovado!" : "Pedido reprovado";
    String body = event.approved()
            ? "Seu pedido: " + event.orderId() + " foi aprovado!"
            : "Seu pedido: " + event.orderId() + " foi reprovado por falta de estoque.";

    NotificationEventEmailDto notificationEventEmailDto = new NotificationEventEmailDto(
            event.to(),
            subject,
            body
    );
    emailSender.sendMessageEmail(notificationEventEmailDto);
  }

  public void processEmailVerification(NotificationEmailVerification event) {
    if (event.to() == null || event.to().isBlank() || event.code() == null || event.code().isBlank()) {
      throw new IllegalArgumentException("Invalid email verification event: to/code must not be null");
    }

    NotificationEventEmailDto notificationEventEmailDto = new NotificationEventEmailDto(
            event.to(),
            "Verificação de e-mail",
            "Por favor, digite o codigo para confirmar seu email: " + event.code()
    );
    emailSender.sendMessageEmail(notificationEventEmailDto);
  }
}
