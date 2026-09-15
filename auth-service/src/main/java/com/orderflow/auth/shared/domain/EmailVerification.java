package com.orderflow.auth.shared.domain;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "tb_email_verification",
        schema = "email_verification"
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EmailVerification {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @OneToOne
  @JoinColumn(
          name = "user_id",
          nullable = false,
          unique = true
  )
  private User user;

  @Column(name = "verification_code", nullable = false, length = 6)
  private String verificationCode;

  @Column(name = "expiration_at", nullable = false)
  private LocalDateTime expirationAt;

  @Column(nullable = false)
  private boolean verified;

  @Column(name="token", nullable = false)
  private String token;


}