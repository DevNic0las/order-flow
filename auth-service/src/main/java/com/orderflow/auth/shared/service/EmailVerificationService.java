package com.orderflow.auth.shared.service;

import com.orderflow.auth.shared.domain.EmailVerification;
import com.orderflow.auth.shared.domain.User;
import com.orderflow.auth.shared.repository.EmailVerificationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

@Service
@RequiredArgsConstructor
public class EmailVerificationService {

  private final EmailVerificationRepository repository;

  public EmailVerification createVerification(User user) {

    String code = generateCode();
    String token = generateToken();

    EmailVerification verification = EmailVerification.builder()
            .user(user)
            .verificationCode(code)
            .token(token)
            .expirationAt(LocalDateTime.now().plusMinutes(10))
            .verified(false)
            .build();

     repository.save(verification);
     return verification;
  }

  public User verify(String token, String code) {

    EmailVerification verification =
            repository.findByToken(token)
                    .orElseThrow(() ->
                            new RuntimeException("Token inválido"));

    if (verification.isVerified()) {
      throw new RuntimeException("E-mail já verificado");
    }

    if (verification.getExpirationAt().isBefore(LocalDateTime.now())) {
      throw new RuntimeException("Código expirado");
    }

    if (!verification.getVerificationCode().equals(code)) {
      throw new RuntimeException("Código inválido");
    }

    verification.setVerified(true);
    repository.save(verification);

    return verification.getUser();
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