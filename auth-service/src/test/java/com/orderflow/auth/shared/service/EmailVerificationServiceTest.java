package com.orderflow.auth.shared.service;

import com.orderflow.auth.shared.domain.EmailVerification;
import com.orderflow.auth.shared.domain.Role;
import com.orderflow.auth.shared.domain.User;
import com.orderflow.auth.shared.exception.EmailAlreadyVerifiedException;
import com.orderflow.auth.shared.exception.InvalidVerificationCodeException;
import com.orderflow.auth.shared.exception.InvalidVerificationTokenException;
import com.orderflow.auth.shared.exception.VerificationCodeExpiredException;
import com.orderflow.auth.shared.repository.EmailVerificationRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EmailVerificationServiceTest {

    @Mock
    private EmailVerificationRepository repository;

    @InjectMocks
    private EmailVerificationService service;

    @Test
    void shouldVerifyTokenSuccessfully() {
        User user = User.builder()
                .email("john@example.com")
                .password("hashed")
                .userName("john")
                .role(Role.CUSTOMER)
                .build();
        EmailVerification verification = EmailVerification.builder()
                .user(user)
                .verificationCode("123456")
                .token("valid-token")
                .expirationAt(LocalDateTime.now().plusMinutes(10))
                .verified(false)
                .build();

        when(repository.findByToken("valid-token")).thenReturn(Optional.of(verification));

        User result = service.verify("valid-token", "123456");

        assertThat(result).isEqualTo(user);

        ArgumentCaptor<EmailVerification> verificationCaptor = ArgumentCaptor.forClass(EmailVerification.class);
        verify(repository).save(verificationCaptor.capture());

        EmailVerification savedVerification = verificationCaptor.getValue();
        assertThat(savedVerification.isVerified()).isTrue();
        assertThat(savedVerification.getUser()).isEqualTo(user);
    }

    @Test
    void shouldRejectWhenTokenDoesNotExist() {
        when(repository.findByToken("missing-token")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.verify("missing-token", "123456"))
                .isInstanceOf(InvalidVerificationTokenException.class)
                .hasMessage("Invalid verification token");
    }

    @Test
    void shouldRejectWhenTokenIsExpired() {
        User user = User.builder()
                .email("john@example.com")
                .password("hashed")
                .userName("john")
                .role(Role.CUSTOMER)
                .build();
        EmailVerification verification = EmailVerification.builder()
                .user(user)
                .verificationCode("123456")
                .token("expired-token")
                .expirationAt(LocalDateTime.now().minusMinutes(1))
                .verified(false)
                .build();

        when(repository.findByToken("expired-token")).thenReturn(Optional.of(verification));

        assertThatThrownBy(() -> service.verify("expired-token", "123456"))
                .isInstanceOf(VerificationCodeExpiredException.class)
                .hasMessage("Verification code expired");
    }

    @Test
    void shouldRejectWhenCodeIsInvalid() {
        User user = User.builder()
                .email("john@example.com")
                .password("hashed")
                .userName("john")
                .role(Role.CUSTOMER)
                .build();
        EmailVerification verification = EmailVerification.builder()
                .user(user)
                .verificationCode("123456")
                .token("coded-token")
                .expirationAt(LocalDateTime.now().plusMinutes(10))
                .verified(false)
                .build();

        when(repository.findByToken("coded-token")).thenReturn(Optional.of(verification));

        assertThatThrownBy(() -> service.verify("coded-token", "654321"))
                .isInstanceOf(InvalidVerificationCodeException.class)
                .hasMessage("Invalid verification code");
    }

    @Test
    void shouldRejectWhenEmailAlreadyVerified() {
        User user = User.builder()
                .email("john@example.com")
                .password("hashed")
                .userName("john")
                .role(Role.CUSTOMER)
                .build();
        EmailVerification verification = EmailVerification.builder()
                .user(user)
                .verificationCode("123456")
                .token("verified-token")
                .expirationAt(LocalDateTime.now().plusMinutes(10))
                .verified(true)
                .build();

        when(repository.findByToken("verified-token")).thenReturn(Optional.of(verification));

        assertThatThrownBy(() -> service.verify("verified-token", "123456"))
                .isInstanceOf(EmailAlreadyVerifiedException.class)
                .hasMessage("Email already verified");
    }

    @Test
    void shouldRejectWhenTokenIsBlank() {
        assertThatThrownBy(() -> service.verify("   ", "123456"))
                .isInstanceOf(InvalidVerificationTokenException.class)
                .hasMessage("Verification token is required");
    }

    @Test
    void shouldRejectWhenTokenIsNull() {
        assertThatThrownBy(() -> service.verify(null, "123456"))
                .isInstanceOf(InvalidVerificationTokenException.class)
                .hasMessage("Verification token is required");
    }

    @Test
    void shouldRejectWhenCodeIsBlank() {
        assertThatThrownBy(() -> service.verify("coded-token", "   "))
                .isInstanceOf(InvalidVerificationCodeException.class)
                .hasMessage("Verification code is required");
    }

    @Test
    void shouldResendVerificationCodeSuccessfully() {
        User user = User.builder()
                .email("john@example.com")
                .password("hashed")
                .userName("john")
                .role(Role.CUSTOMER)
                .build();
        EmailVerification verification = EmailVerification.builder()
                .user(user)
                .verificationCode("123456")
                .token("refresh-token")
                .expirationAt(LocalDateTime.now().plusMinutes(2))
                .lastSentAt(LocalDateTime.now().minusMinutes(5))
                .verified(false)
                .build();

        when(repository.findByToken("refresh-token")).thenReturn(Optional.of(verification));
        when(repository.save(verification)).thenReturn(verification);

        EmailVerification result = service.resendVerification("refresh-token");

        assertThat(result).isEqualTo(verification);
        assertThat(result.getVerificationCode()).isNotBlank();
        assertThat(result.getVerificationCode()).isNotEqualTo("123456");
        assertThat(result.getExpirationAt()).isAfter(LocalDateTime.now());

        ArgumentCaptor<EmailVerification> verificationCaptor = ArgumentCaptor.forClass(EmailVerification.class);
        verify(repository).save(verificationCaptor.capture());
        assertThat(verificationCaptor.getValue()).isEqualTo(verification);
    }

    @Test
    void shouldRejectWhenTokenIsInvalid() {
        when(repository.findByToken("invalid-token"))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.resendVerification("invalid-token"))
                .isInstanceOf(InvalidVerificationTokenException.class)
                .hasMessage("Invalid verification token");

        verify(repository, never()).save(any());
    }



}
