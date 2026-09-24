package com.orderflow.auth.shared.exception;

public class VerificationCooldownException extends RuntimeException {
  public VerificationCooldownException(String message) {
    super(message);
  }
}
