package com.orderflow.notification.port;

import com.orderflow.notification.dto.NotificationEventEmailDto;

public interface EmailSender {


  void sendMessageEmail(NotificationEventEmailDto notificationEventEmailDto);


}
