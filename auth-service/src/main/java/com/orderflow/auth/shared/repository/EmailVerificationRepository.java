package com.orderflow.auth.shared.repository;

import com.orderflow.auth.shared.domain.EmailVerification;
import com.orderflow.auth.shared.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface EmailVerificationRepository
        extends JpaRepository<EmailVerification, Long> {

  Optional<EmailVerification> findByUser(User user);
}