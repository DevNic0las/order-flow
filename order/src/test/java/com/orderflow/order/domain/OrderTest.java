package com.orderflow.order.domain;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class OrderTest {

    @Test
    void shouldKeepConfirmedStatusWhenConfirmedAgain() {
        Order order = new Order();
        order.setStatus(OrderStatus.CONFIRMED);

        assertDoesNotThrow(order::confirmed);
        assertEquals(OrderStatus.CONFIRMED, order.getStatus());
    }

    @Test
    void shouldKeepRejectedStatusWhenConfirmedAfterRejection() {
        Order order = new Order();
        order.setStatus(OrderStatus.REJECTED);

        assertDoesNotThrow(order::confirmed);
        assertEquals(OrderStatus.REJECTED, order.getStatus());
    }

    @Test
    void shouldKeepRejectedStatusWhenRejectedAgain() {
        Order order = new Order();
        order.setStatus(OrderStatus.REJECTED);

        assertDoesNotThrow(order::reject);
        assertEquals(OrderStatus.REJECTED, order.getStatus());
    }

    @Test
    void shouldKeepConfirmedStatusWhenRejectedAfterConfirmation() {
        Order order = new Order();
        order.setStatus(OrderStatus.CONFIRMED);

        assertDoesNotThrow(order::reject);
        assertEquals(OrderStatus.CONFIRMED, order.getStatus());
    }

    @Test
    void shouldConfirmPendingOrder() {
        Order order = new Order();
        order.setStatus(OrderStatus.PENDING);

        assertDoesNotThrow(order::confirmed);
        assertEquals(OrderStatus.CONFIRMED, order.getStatus());
    }

    @Test
    void shouldRejectPendingOrder() {
        Order order = new Order();
        order.setStatus(OrderStatus.PENDING);

        assertDoesNotThrow(order::reject);
        assertEquals(OrderStatus.REJECTED, order.getStatus());
    }
}
