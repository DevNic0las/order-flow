package com.orderflow.web.dto;

public record ProductViewDto(
        Long id,
        String productName,
        Integer quantity
) {
}
