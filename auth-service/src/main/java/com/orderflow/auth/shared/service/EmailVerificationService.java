package com.orderflow.auth.shared.service;

import com.orderflow.auth.shared.domain.EmailVerification;
import com.orderflow.auth.shared.domain.User;
import com.orderflow.auth.shared.exception.*;
import com.orderflow.auth.shared.repository.EmailVerificationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class EmailVerificationService {

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private final EmailVerificationRepository repository;

    public EmailVerification createVerification(User user) {
        if (user == null) {
            throw new InvalidRequestException("User is required");
        }

        String code = generateCode();
        String token = generateToken();
        LocalDateTime now = LocalDateTime.now();

        EmailVerification verification = EmailVerification.builder()
                .user(user)
                .verificationCode(code)
                .token(token)
                .expirationAt(now.plusMinutes(10))
                .lastSentAt(now)
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

        if (!codeMatches(verification.getVerificationCode(), code)) {
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
                .orElseThrow(() ->
                        new InvalidVerificationTokenException("Invalid verification token"));

        if (verification.isVerified()) {
            throw new EmailAlreadyVerifiedException("Email already verified");
        }

        LocalDateTime now = LocalDateTime.now();

        if (verification.getLastSentAt() != null
                && verification.getLastSentAt().plusMinutes(1).isAfter(now)) {
            throw new VerificationCooldownException(

                    "Please wait before requesting another verification code"
            );
        }

        verification.setVerificationCode(generateCode());
        verification.setExpirationAt(now.plusMinutes(10));
        verification.setLastSentAt(now);

        return repository.save(verification);
    }

    private String generateCode() {
        return String.valueOf(SECURE_RANDOM.nextInt(100000, 1000000));
    }

    private boolean codeMatches(String expected, String provided) {
        return MessageDigest.isEqual(
                expected.getBytes(StandardCharsets.UTF_8),
                provided.getBytes(StandardCharsets.UTF_8)
        );
    }

    private String generateToken() {
        return UUID.randomUUID().toString();
    }
}