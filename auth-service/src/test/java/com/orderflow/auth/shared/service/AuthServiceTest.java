package com.orderflow.auth.shared.service;

import com.orderflow.auth.shared.domain.EmailVerification;
import com.orderflow.auth.shared.domain.Role;
import com.orderflow.auth.shared.domain.User;
import com.orderflow.auth.shared.dto.*;
import com.orderflow.auth.shared.exception.EmailAlreadyExistsException;
import com.orderflow.auth.shared.exception.InvalidCredentialsException;
import com.orderflow.auth.shared.exception.InvalidRequestException;
import com.orderflow.auth.shared.exception.UsernameAlreadyExistsException;
import com.orderflow.auth.shared.messaging.EmailVerificationProducer;
import com.orderflow.auth.shared.repository.EmailVerificationRepository;
import com.orderflow.auth.shared.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private EmailVerificationRepository emailVerificationRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtTokenGenerator jwtTokenGenerator;

    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private EmailVerificationService emailVerificationService;

    @Mock
    private EmailVerificationProducer emailVerificationProducer;

    @InjectMocks
    private AuthService authService;

    @Test
    void shouldRegisterUserSuccessfully() {
        RegisterRequestDto request = new RegisterRequestDto("john@example.com", "secret123", "john");
        User user = User.builder()
                .email("john@example.com")
                .password("hashed")
                .userName("john")
                .role(Role.CUSTOMER)
                .build();
        EmailVerification verification = EmailVerification.builder()
                .user(user)
                .verificationCode("123456")
                .token("verification-token")
                .verified(false)
                .build();

        when(userRepository.existsByEmail("john@example.com")).thenReturn(false);
        when(userRepository.existsByUserName("john")).thenReturn(false);
        when(passwordEncoder.encode("secret123")).thenReturn("hashed");
        when(emailVerificationService.createVerification(any(User.class))).thenReturn(verification);

        RegisterResponseDto response = authService.register(request);

        assertThat(response.token()).isEqualTo("verification-token");

        ArgumentCaptor<User> savedUserCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(savedUserCaptor.capture());

        User savedUser = savedUserCaptor.getValue();
        assertThat(savedUser.getEmail()).isEqualTo("john@example.com");
        assertThat(org.springframework.test.util.ReflectionTestUtils.getField(savedUser, "userName")).isEqualTo("john");
        assertThat(savedUser.getPassword()).isEqualTo("hashed");
        assertThat(savedUser.getPassword()).isNotEqualTo("secret123");
        assertThat(savedUser.getRole()).isEqualTo(Role.CUSTOMER);

        ArgumentCaptor<User> verificationUserCaptor = ArgumentCaptor.forClass(User.class);
        verify(emailVerificationService).createVerification(verificationUserCaptor.capture());
        assertThat(verificationUserCaptor.getValue()).isEqualTo(savedUser);

        ArgumentCaptor<EmailVerificationEventDto> eventCaptor = ArgumentCaptor.forClass(EmailVerificationEventDto.class);
        verify(emailVerificationProducer).send(eventCaptor.capture());

        EmailVerificationEventDto event = eventCaptor.getValue();
        assertThat(event.to()).isEqualTo("john@example.com");
        assertThat(event.code()).isEqualTo("123456");
    }

    @Test
    void shouldRejectRegistrationWhenEmailAlreadyExists() {
        RegisterRequestDto request = new RegisterRequestDto("john@example.com", "secret123", "john");

        when(userRepository.existsByEmail("john@example.com")).thenReturn(true);

        assertThatThrownBy(() -> authService.register(request))
                .isInstanceOf(EmailAlreadyExistsException.class)
                .hasMessage("Email already registered");

        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void shouldRejectRegistrationWhenUsernameAlreadyExists() {
        RegisterRequestDto request = new RegisterRequestDto("john@example.com", "secret123", "john");

        when(userRepository.existsByEmail("john@example.com")).thenReturn(false);
        when(userRepository.existsByUserName("john")).thenReturn(true);

        assertThatThrownBy(() -> authService.register(request))
                .isInstanceOf(UsernameAlreadyExistsException.class)
                .hasMessage("Username already registered");

        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void shouldRejectRegistrationWhenRequiredFieldsAreNull() {
        assertThatThrownBy(() -> authService.register(new RegisterRequestDto(null, "secret123", "john")))
                .isInstanceOf(InvalidRequestException.class)
                .hasMessage("Email, username and password are required");

        assertThatThrownBy(() -> authService.register(new RegisterRequestDto("john@example.com", null, "john")))
                .isInstanceOf(InvalidRequestException.class)
                .hasMessage("Email, username and password are required");

        assertThatThrownBy(() -> authService.register(new RegisterRequestDto("john@example.com", "secret123", null)))
                .isInstanceOf(InvalidRequestException.class)
                .hasMessage("Email, username and password are required");
    }

    @Test
    void shouldRejectRegistrationWhenRequiredFieldsAreBlank() {
        assertThatThrownBy(() -> authService.register(new RegisterRequestDto("   ", "secret123", "john")))
                .isInstanceOf(InvalidRequestException.class)
                .hasMessage("Email, username and password are required");

        assertThatThrownBy(() -> authService.register(new RegisterRequestDto("john@example.com", "   ", "john")))
                .isInstanceOf(InvalidRequestException.class)
                .hasMessage("Email, username and password are required");

        assertThatThrownBy(() -> authService.register(new RegisterRequestDto("john@example.com", "secret123", "   ")))
                .isInstanceOf(InvalidRequestException.class)
                .hasMessage("Email, username and password are required");
    }

    @Test
    void shouldHashPasswordAndCreateVerification() {
        RegisterRequestDto request = new RegisterRequestDto("john@example.com", "secret123", "john");
        User user = User.builder()
                .email("john@example.com")
                .password("hashed")
                .userName("john")
                .role(Role.CUSTOMER)
                .build();
        EmailVerification verification = EmailVerification.builder()
                .user(user)
                .verificationCode("123456")
                .token("verification-token")
                .verified(false)
                .build();

        when(userRepository.existsByEmail("john@example.com")).thenReturn(false);
        when(userRepository.existsByUserName("john")).thenReturn(false);
        when(passwordEncoder.encode("secret123")).thenReturn("hashed");
        when(emailVerificationService.createVerification(any(User.class))).thenReturn(verification);

        authService.register(request);

        ArgumentCaptor<User> savedUserCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(savedUserCaptor.capture());

        User savedUser = savedUserCaptor.getValue();
        assertThat(savedUser.getPassword()).isEqualTo("hashed");
        assertThat(savedUser.getPassword()).isNotEqualTo(request.password());
        verify(passwordEncoder).encode("secret123");

        ArgumentCaptor<User> verificationUserCaptor = ArgumentCaptor.forClass(User.class);
        verify(emailVerificationService).createVerification(verificationUserCaptor.capture());
        assertThat(verificationUserCaptor.getValue()).isEqualTo(savedUser);
    }

    @Test
    void shouldThrowWhenPersistenceFails() {
        RegisterRequestDto request = new RegisterRequestDto("john@example.com", "secret123", "john");

        when(userRepository.existsByEmail("john@example.com")).thenReturn(false);
        when(userRepository.existsByUserName("john")).thenReturn(false);
        when(passwordEncoder.encode("secret123")).thenReturn("hashed");
        when(userRepository.save(any(User.class))).thenThrow(new DataIntegrityViolationException("duplicate"));

        assertThatThrownBy(() -> authService.register(request))
                .isInstanceOf(EmailAlreadyExistsException.class)
                .hasMessage("Email already registered");
    }

    @Test
    void shouldAuthenticateUserSuccessfully() {
        LoginRequestDto request = new LoginRequestDto("john@example.com", "secret123");
        User user = User.builder()
                .email("john@example.com")
                .password("hashed")
                .userName("john")
                .role(Role.CUSTOMER)
                .build();

        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(new UsernamePasswordAuthenticationToken("john@example.com", "secret123"));
        when(userRepository.findByEmail("john@example.com")).thenReturn(Optional.of(user));
        when(jwtTokenGenerator.generateToken(user)).thenReturn("jwt-token");

        AuthResponseDto response = authService.login(request);

        assertThat(response.token()).isEqualTo("jwt-token");
    }

    @Test
    void shouldVerifyUserSuccessfully() {
        RegisterRequestEmailDto request = new RegisterRequestEmailDto("token-123", "123456");
        User user = User.builder()
                .email("john@example.com")
                .password("hashed")
                .userName("john")
                .role(Role.CUSTOMER)
                .build();

        when(emailVerificationService.verify("token-123", "123456")).thenReturn(user);
        when(jwtTokenGenerator.generateToken(user)).thenReturn("jwt-verified");

        AuthResponseDto response = authService.verify(request);

        assertThat(response.token()).isEqualTo("jwt-verified");
    }

    @Test
    void shouldRejectLoginWhenCredentialsInvalid() {
        LoginRequestDto request = new LoginRequestDto("john@example.com", "wrong-password");

        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenThrow(new org.springframework.security.authentication.BadCredentialsException("bad creds"));

        assertThatThrownBy(() -> authService.login(request))
                .isInstanceOf(InvalidCredentialsException.class)
                .hasMessage("Invalid credentials");
    }

    @Test
    void shouldRejectLoginWhenUserNotFoundAfterAuthentication() {
        LoginRequestDto request = new LoginRequestDto("john@example.com", "secret123");

        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(new UsernamePasswordAuthenticationToken("john@example.com", "secret123"));
        when(userRepository.findByEmail("john@example.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.login(request))
                .isInstanceOf(InvalidCredentialsException.class)
                .hasMessage("Invalid credentials");
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
                .verificationCode("111111")
                .token("token-123")
                .verified(false)
                .build();

        when(emailVerificationService.resendVerification("token-123")).thenReturn(verification);

        ResendCodeResponseDto response = authService.resendCode(new ResendCodeRequestDto(UUID.randomUUID().toString()));

        assertThat(response).isEqualTo("token-123");

        ArgumentCaptor<EmailVerificationEventDto> eventCaptor = ArgumentCaptor.forClass(EmailVerificationEventDto.class);
        verify(emailVerificationProducer).send(eventCaptor.capture());
        assertThat(eventCaptor.getValue().to()).isEqualTo("john@example.com");
        assertThat(eventCaptor.getValue().code()).isEqualTo("111111");
    }

    @Test
    void shouldRejectResendWhenTokenIsBlank() {
        assertThatThrownBy(() -> authService.resendCode(new ResendCodeRequestDto(UUID.randomUUID().toString()   )))
                .isInstanceOf(InvalidRequestException.class)
                .hasMessage("Token is required");
    }
}
