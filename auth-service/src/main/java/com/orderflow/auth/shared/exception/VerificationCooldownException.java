package com.orderflow.auth.shared.exception;

import lombok.Getter;

@Getter
public class VerificationCooldownException extends RuntimeException {

  private final long retryAfterSeconds;

  public VerificationCooldownException(String message, long retryAfterSeconds) {
    super(message);
    this.retryAfterSeconds = retryAfterSeconds;
  }
}
