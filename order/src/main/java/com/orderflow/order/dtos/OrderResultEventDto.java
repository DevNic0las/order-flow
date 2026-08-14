package com.orderflow.order.dtos;

public record OrderResultEventDto(
        Long orderId,
        boolean approved
) {
}
