package com.overflow.inventory.exception;

import java.time.LocalDateTime;
import java.util.Map;

public record InventoryErrorResponseDto(
        int status,
        String message,
        LocalDateTime timestamp,
        Map<String, String> errors
) {
}
