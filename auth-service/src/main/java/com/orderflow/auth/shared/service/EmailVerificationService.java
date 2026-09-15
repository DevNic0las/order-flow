package com.orderflow.auth.shared.service;

import com.orderflow.auth.shared.domain.EmailVerification;
import com.orderflow.auth.shared.domain.User;
import com.orderflow.auth.shared.repository.EmailVerificationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.concurrent.ThreadLocalRandom;

@Service
@RequiredArgsConstructor
public class EmailVerificationService {

  private final EmailVerificationRepository repository;

  public String createVerification(User user) {

    String code = generateCode();

    EmailVerification verification = EmailVerification.builder()
            .user(user)
            .verificationCode(code)
            .expirationAt(LocalDateTime.now().plusMinutes(10))
            .verified(false)
            .build();

    repository.save(verification);

    return code;
  }

  private String generateCode() {
    return String.valueOf(
            ThreadLocalRandom.current().nextInt(100000, 1000000)
    );
  }
}