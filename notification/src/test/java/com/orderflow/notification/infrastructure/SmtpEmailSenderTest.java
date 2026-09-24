package com.orderflow.notification.infrastructure;

import com.orderflow.notification.dto.NotificationEventEmailDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;

import java.util.Objects;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class SmtpEmailSenderTest {

    @Mock
    private JavaMailSender mailSender;

    private SmtpEmailSender smtpEmailSender;

    @BeforeEach
    void setUp() {
        smtpEmailSender = new SmtpEmailSender(mailSender);
    }

    @Test
    @DisplayName("Should send email successfully via JavaMailSender")
    void shouldSendEmailSuccessfullyViaJavaMailSender() {
        NotificationEventEmailDto event = new NotificationEventEmailDto("user@example.com", "Test Subject", "Test Body");

        smtpEmailSender.sendMessageEmail(event);

        ArgumentCaptor<SimpleMailMessage> captor = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(mailSender).send(captor.capture());

        SimpleMailMessage captured = captor.getValue();
        assertEquals("user@example.com", Objects.requireNonNull(captured.getTo())[0]);
        assertEquals("Test Subject", captured.getSubject());
        assertEquals("Test Body", captured.getText());
    }

    @Test
    @DisplayName("Should throw IllegalArgumentException when fields are null")
    void shouldThrowIllegalArgumentExceptionWhenFieldsAreNull() {
        assertThrows(IllegalArgumentException.class, () ->
                smtpEmailSender.sendMessageEmail(new NotificationEventEmailDto(null, "Subject", "Body")));
        assertThrows(IllegalArgumentException.class, () ->
                smtpEmailSender.sendMessageEmail(new NotificationEventEmailDto("user@example.com", null, "Body")));
        assertThrows(IllegalArgumentException.class, () ->
                smtpEmailSender.sendMessageEmail(new NotificationEventEmailDto("user@example.com", "Subject", null)));
    }
}
