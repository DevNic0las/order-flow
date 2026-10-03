package com.orderflow.order.outbox;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import com.orderflow.order.IntegrationTestBase;
import com.orderflow.order.dtos.OrderEventDto;
import com.orderflow.order.dtos.OrderRequestDto;
import com.orderflow.order.messaging.OrderPublisher;
import com.orderflow.order.repository.OrderRepository;
import com.orderflow.order.service.OrderService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;

class OutboxPublisherIntegrationTest extends IntegrationTestBase {

    @Autowired
    private OrderService orderService;

    @Autowired
    private OutboxEventRepository outboxEventRepository;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private OutboxPublisher outboxPublisher;

    @MockBean
    private OrderPublisher orderPublisher;

    @Test
    void shouldPersistOutboxEventAndPublishIt() {
        OrderRequestDto request = new OrderRequestDto(42L, 3);

        orderService.createOrder(request, "user-1");

        var orders = orderRepository.findAll();
        assertEquals(1, orders.size());

        var outboxEvents = outboxEventRepository.findAll();
        assertEquals(1, outboxEvents.size());
        assertEquals(OutboxEventStatus.PENDING, outboxEvents.get(0).getStatus());
        assertNotNull(outboxEvents.get(0).getEventId());

        outboxPublisher.publishPendingEvents();

        var updated = outboxEventRepository.findAll();
        assertEquals(1, updated.size());
        assertEquals(OutboxEventStatus.PUBLISHED, updated.get(0).getStatus());
        assertNotNull(updated.get(0).getPublishedAt());

        verify(orderPublisher).publishOrder(any(OrderEventDto.class));
    }
}
