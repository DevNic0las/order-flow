package com.orderflow.web.dto;

import java.time.LocalDateTime;

public record OrderViewDto(
        Long id,
        String customerName,
        Integer quantity,
        String status,
        LocalDateTime createdAt
) {
}
