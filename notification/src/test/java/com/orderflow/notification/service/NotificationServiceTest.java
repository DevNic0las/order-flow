package com.orderflow.notification.service;

import com.orderflow.notification.dto.NotificationEmailVerification;
import com.orderflow.notification.dto.NotificationEventDto;
import com.orderflow.notification.dto.NotificationEventEmailDto;
import com.orderflow.notification.port.EmailSender;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {

    @Mock
    private EmailSender emailSender;

    private NotificationService notificationService;

    @BeforeEach
    void setUp() {
        notificationService = new NotificationService(emailSender);
    }

    @Test
    @DisplayName("Should send approval email when order is approved")
    void shouldSendApprovalEmailWhenOrderIsApproved() {
        UUID eventId = UUID.randomUUID();
        NotificationEventDto event = new NotificationEventDto(123L, true, "customer@example.com", eventId);

        notificationService.processNotification(event);

        ArgumentCaptor<NotificationEventEmailDto> captor = ArgumentCaptor.forClass(NotificationEventEmailDto.class);
        verify(emailSender).sendMessageEmail(captor.capture());

        NotificationEventEmailDto sentEmail = captor.getValue();
        assertEquals("customer@example.com", sentEmail.to());
        assertEquals("Pedido aprovado!", sentEmail.subject());
        assertTrue(sentEmail.body().contains("123"));
        assertTrue(sentEmail.body().contains("aprovado"));
    }

    @Test
    @DisplayName("Should send rejection email when order is not approved")
    void shouldSendRejectionEmailWhenOrderIsNotApproved() {
        UUID eventId = UUID.randomUUID();
        NotificationEventDto event = new NotificationEventDto(456L, false, "customer@example.com", eventId);

        notificationService.processNotification(event);

        ArgumentCaptor<NotificationEventEmailDto> captor = ArgumentCaptor.forClass(NotificationEventEmailDto.class);
        verify(emailSender).sendMessageEmail(captor.capture());

        NotificationEventEmailDto sentEmail = captor.getValue();
        assertEquals("customer@example.com", sentEmail.to());
        assertEquals("Pedido reprovado", sentEmail.subject());
        assertTrue(sentEmail.body().contains("456"));
        assertTrue(sentEmail.body().contains("reprovado"));
    }

    @Test
    @DisplayName("Should send verification email with OTP code")
    void shouldSendVerificationEmailWithOtpCode() {
        NotificationEmailVerification event = new NotificationEmailVerification("987654", "user@example.com");

        notificationService.processEmailVerification(event);

        ArgumentCaptor<NotificationEventEmailDto> captor = ArgumentCaptor.forClass(NotificationEventEmailDto.class);
        verify(emailSender).sendMessageEmail(captor.capture());

        NotificationEventEmailDto sentEmail = captor.getValue();
        assertEquals("user@example.com", sentEmail.to());
        assertEquals("Verificação de e-mail", sentEmail.subject());
        assertTrue(sentEmail.body().contains("987654"));
    }
}
