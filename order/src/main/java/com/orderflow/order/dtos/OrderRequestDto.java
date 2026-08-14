package com.orderflow.order.dtos;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record OrderRequestDto(
        @NotBlank String customerName,
        @NotNull Long productId,
        @NotNull @Positive Integer quantity
) {
}
