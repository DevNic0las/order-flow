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
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {
    private final UserRepository userRepository;
    private final EmailVerificationRepository repository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenGenerator jwtService;
    private final AuthenticationManager authenticationManager;
    private final EmailVerificationService emailVerificationService;
    private final EmailVerificationProducer emailVerificationProducer;

    public AuthService(UserRepository userRepository, PasswordEncoder passwordEncoder,
                       JwtTokenGenerator jwtService, AuthenticationManager authenticationManager,
                       EmailVerificationService emailVerificationService,
                       EmailVerificationProducer emailVerificationProducer,
                       EmailVerificationRepository repository) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.authenticationManager = authenticationManager;
        this.emailVerificationService = emailVerificationService;
        this.emailVerificationProducer = emailVerificationProducer;
        this.repository = repository;
    }

    @Transactional
    public RegisterResponseDto register(RegisterRequestDto request) {
        if (request == null) {
            throw new InvalidRequestException("Request body is required");
        }

        String email = request.email();
        String username = request.username();
        String password = request.password();

        if (email == null || email.isBlank() || username == null || username.isBlank() || password == null || password.isBlank()) {
            throw new InvalidRequestException("Email, username and password are required");
        }

        if (userRepository.existsByEmail(email)) {
            throw new EmailAlreadyExistsException("Email already registered");
        }

        if (userRepository.existsByUserName(username)) {
            throw new UsernameAlreadyExistsException("Username already registered");
        }

        User user = User.builder()
                .email(email)
                .password(passwordEncoder.encode(password))
                .userName(username)
                .role(Role.CUSTOMER)
                .build();

        try {
            userRepository.save(user);
        } catch (DataIntegrityViolationException ex) {
            throw new EmailAlreadyExistsException("Email already registered");
        }

        EmailVerification verification = emailVerificationService.createVerification(user);
        emailVerificationProducer.send(new EmailVerificationEventDto(user.getEmail(), verification.getVerificationCode()));

        return new RegisterResponseDto(verification.getToken());
    }

    public AuthResponseDto login(LoginRequestDto request) {
        if (request == null) {
            throw new InvalidRequestException("Request body is required");
        }

        if (request.email() == null || request.email().isBlank() || request.password() == null || request.password().isBlank()) {
            throw new InvalidRequestException("Email and password are required");
        }

        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.email(), request.password())
            );
        } catch (AuthenticationException ex) {
            throw new InvalidCredentialsException();
        }

        User user = userRepository.findByEmail(request.email())
                .orElseThrow(InvalidCredentialsException::new);

        return new AuthResponseDto(jwtService.generateToken(user));
    }

    public AuthResponseDto verify(RegisterRequestEmailDto request) {
        if (request == null) {
            throw new InvalidRequestException("Request body is required");
        }

        User user = emailVerificationService.verify(request.token(), request.code());
        return new AuthResponseDto(jwtService.generateToken(user));
    }

    public ResendCodeResponseDto resendCode(ResendCodeRequestDto request) {
        if (request == null) {
            throw new InvalidRequestException("Request body is required");
        }

        if (request.token() == null || request.token().isBlank()) {
            throw new InvalidRequestException("Token is required");
        }

        EmailVerification verification = emailVerificationService.resendVerification(request.token());
        emailVerificationProducer.send(new EmailVerificationEventDto(
                verification.getUser().getEmail(),
                verification.getVerificationCode()
        ));

        return new ResendCodeResponseDto(verification.getToken());
    }



}
