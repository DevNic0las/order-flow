package com.orderflow.auth.shared.service;

import com.orderflow.auth.shared.domain.EmailVerification;
import com.orderflow.auth.shared.domain.User;
import com.orderflow.auth.shared.exception.EmailAlreadyVerifiedException;
import com.orderflow.auth.shared.exception.InvalidRequestException;
import com.orderflow.auth.shared.exception.InvalidVerificationCodeException;
import com.orderflow.auth.shared.exception.InvalidVerificationTokenException;
import com.orderflow.auth.shared.exception.VerificationCodeExpiredException;
import com.orderflow.auth.shared.repository.EmailVerificationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

@Service
@RequiredArgsConstructor
public class EmailVerificationService {

    private final EmailVerificationRepository repository;

    public EmailVerification createVerification(User user) {
        if (user == null) {
            throw new InvalidRequestException("User is required");
        }

        String code = generateCode();
        String token = generateToken();

        EmailVerification verification = EmailVerification.builder()
                .user(user)
                .verificationCode(code)
                .token(token)
                .expirationAt(LocalDateTime.now().plusMinutes(10))
                .verified(false)
                .build();

        try {
            repository.save(verification);
        } catch (DataIntegrityViolationException ex) {
            throw new InvalidRequestException("Email verification could not be created");
        }

        return verification;
    }

    public User verify(String token, String code) {
        if (token == null || token.isBlank()) {
            throw new InvalidVerificationTokenException("Verification token is required");
        }

        if (code == null || code.isBlank()) {
            throw new InvalidVerificationCodeException("Verification code is required");
        }

        EmailVerification verification = repository.findByToken(token)
                .orElseThrow(() -> new InvalidVerificationTokenException("Invalid verification token"));

        if (verification.isVerified()) {
            throw new EmailAlreadyVerifiedException("Email already verified");
        }

        if (verification.getExpirationAt().isBefore(LocalDateTime.now())) {
            throw new VerificationCodeExpiredException("Verification code expired");
        }

        if (!verification.getVerificationCode().equals(code)) {
            throw new InvalidVerificationCodeException("Invalid verification code");
        }

        verification.setVerified(true);
        repository.save(verification);

        return verification.getUser();
    }

    public EmailVerification resendVerification(String token) {
        if (token == null || token.isBlank()) {
            throw new InvalidVerificationTokenException("Verification token is required");
        }

        EmailVerification verification = repository.findByToken(token)
                .orElseThrow(() -> new InvalidVerificationTokenException("Invalid verification token"));

        if (verification.isVerified()) {
            throw new EmailAlreadyVerifiedException("Email already verified");
        }

        verification.setVerificationCode(generateCode());
        verification.setExpirationAt(LocalDateTime.now().plusMinutes(10));
        repository.save(verification);

        return verification;
    }

    private String generateCode() {
        return String.valueOf(
                ThreadLocalRandom.current()
                        .nextInt(100000, 1000000)
        );
    }

    private String generateToken() {
        return UUID.randomUUID().toString();
    }
}