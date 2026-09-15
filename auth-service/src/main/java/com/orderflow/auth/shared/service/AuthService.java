package com.orderflow.auth.shared.service;

import com.orderflow.auth.shared.domain.EmailVerification;
import com.orderflow.auth.shared.domain.Role;
import com.orderflow.auth.shared.domain.User;
import com.orderflow.auth.shared.dto.*;
import com.orderflow.auth.shared.messaging.EmailVerificationProducer;
import com.orderflow.auth.shared.repository.EmailVerificationRepository;
import com.orderflow.auth.shared.repository.UserRepository;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

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
                       EmailVerificationRepository repository

    ) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.authenticationManager = authenticationManager;
        this.emailVerificationService = emailVerificationService;
        this.emailVerificationProducer = emailVerificationProducer;
    this.repository = repository;
    }

    public RegisterResponseDto register(RegisterRequestDto request) {
        User user = User.builder()
                .email(request.email())
                .password(passwordEncoder.encode(request.password()))
                .userName(request.username())
                .role(Role.CUSTOMER)
                .build();
        userRepository.save(user);


        EmailVerification verification =
                emailVerificationService.createVerification(user);

        emailVerificationProducer.send(new EmailVerificationEventDto(user.getEmail(), verification.getVerificationCode()));

        return new RegisterResponseDto(verification.getToken());
    }

    public AuthResponseDto login(LoginRequestDto request) {
        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.email(), request.password())
            );
        } catch (org.springframework.security.core.AuthenticationException ex) {
            // Translate authentication failures to 400 Bad Request for login attempts
            throw new org.springframework.web.server.ResponseStatusException(
                    org.springframework.http.HttpStatus.BAD_REQUEST,
                    "Invalid credentials",
                    ex
            );
        }

        User user = userRepository.findByEmail(request.email())
                .orElseThrow();

        return new AuthResponseDto(jwtService.generateToken(user));
    }
    public AuthResponseDto verify(RegisterRequestEmailDto request) {

        User user = emailVerificationService.verify(
                request.token(),
                request.code()
        );

        return new AuthResponseDto(
                jwtService.generateToken(user)
        );
    }


}
