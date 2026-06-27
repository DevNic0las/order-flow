package com.overflow.order.exception;

import java.time.LocalDateTime;
import java.util.Map;

public record ErrorResponseDto(
        int status,
        String message,
        LocalDateTime timestamp,
        Map<String, String> errors
) {
  public ErrorResponseDto(int status, String message, LocalDateTime timestamp) {
    this(status, message, timestamp, null);
  }
}
