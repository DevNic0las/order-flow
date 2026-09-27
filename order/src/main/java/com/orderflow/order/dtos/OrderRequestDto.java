package com.orderflow.order.dtos;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record OrderRequestDto(
        @NotNull Long productId,
        @NotNull @Positive Integer quantity
) {
}
