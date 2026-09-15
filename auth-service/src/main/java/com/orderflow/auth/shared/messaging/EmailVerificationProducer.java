package com.orderflow.auth.shared.messaging;


import com.orderflow.auth.shared.config.RabbitmqConfig;
import com.orderflow.auth.shared.dto.EmailVerificationEventDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class EmailVerificationProducer {
  private final RabbitTemplate rabbitTemplate;

  public void send(EmailVerificationEventDto event) {

    log.info(
            "Sending email verification event for {}",
            event.to()
    );

    rabbitTemplate.convertAndSend(
            RabbitmqConfig.NOTIFICATION_EXCHANGE,
            RabbitmqConfig.RK_EMAIL_VERIFICATION,
            event
    );
  }


}
