package com.overflow.order.dtos;

public record OrderResultEventDto(
        Long orderId,
        boolean approved
) {
}
