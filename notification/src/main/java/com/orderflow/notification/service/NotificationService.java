package com.orderflow.notification.service;

import com.orderflow.notification.dto.NotificationEventDto;
import com.orderflow.notification.dto.NotificationEventEmailDto;
import com.orderflow.notification.infrastructure.BrevoEmailSender;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationService {

  private final BrevoEmailSender brevoEmailSender;

  public void processNotification(NotificationEventDto event) {

    NotificationEventEmailDto notificationEventEmailDto = new NotificationEventEmailDto(
            event.to(),
            "Pedido aprovado!",
            "Seu pedido: " + event.orderId() + " foi aprovado!"
    );
    brevoEmailSender.sendMessageEmail(notificationEventEmailDto);
  }


}
