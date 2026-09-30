package com.orderflow.order.integration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import com.orderflow.order.IntegrationTestBase;
import com.orderflow.order.domain.Order;
import com.orderflow.order.domain.OrderStatus;
import com.orderflow.order.dtos.OrderResultEventDto;
import com.orderflow.order.messaging.OrderConsumer;
import com.orderflow.order.repository.OrderRepository;
import com.orderflow.order.repository.ProcessedOrderResultEventRepository;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;

class OrderIdempotencyRaceIntegrationTest extends IntegrationTestBase {

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private ProcessedOrderResultEventRepository processedOrderResultEventRepository;

    @Autowired
    private OrderConsumer orderConsumer;

    @MockBean
    private com.orderflow.order.messaging.OrderPublisher orderPublisher;

    private Long createdOrderId;

    @AfterEach
    void cleanup() {
        processedOrderResultEventRepository.deleteAll();
        if (createdOrderId != null) {
            orderRepository.deleteById(createdOrderId);
            createdOrderId = null;
        }
    }

    @Test
    void concurrentSameEventId_shouldProcessOnlyOnce() throws Exception {
        Order order = new Order();
        order.setCustomerName("Ana");
        order.setProductId(42L);
        order.setQuantity(3);
        Order savedOrder = orderRepository.saveAndFlush(order);
        createdOrderId = savedOrder.getId();

        UUID eventId = UUID.randomUUID();
        OrderResultEventDto event = new OrderResultEventDto(savedOrder.getId(), true, eventId);

        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);
        AtomicReference<Throwable> t1Ex = new AtomicReference<>();
        AtomicReference<Throwable> t2Ex = new AtomicReference<>();

        Thread t1 = new Thread(() -> {
            try {
                ready.countDown();
                start.await();
                orderConsumer.onOrderResult(event);
            } catch (Throwable t) {
                t1Ex.set(t);
            }
        });

        Thread t2 = new Thread(() -> {
            try {
                ready.countDown();
                start.await();
                orderConsumer.onOrderResult(event);
            } catch (Throwable t) {
                t2Ex.set(t);
            }
        });

        t1.start();
        t2.start();

        ready.await();
        start.countDown();

        t1.join();
        t2.join();

        Order finalOrder = orderRepository.findById(savedOrder.getId()).orElseThrow();
        assertEquals(OrderStatus.CONFIRMED, finalOrder.getStatus());

        long rows = processedOrderResultEventRepository.findAll().stream()
            .filter(e -> e.getEventId().equals(eventId))
            .count();
        assertEquals(1L, rows, "Exactly one processed-result row should exist for the eventId");
        assertNull(t1Ex.get(), "First consumer thread should not fail");
        assertNull(t2Ex.get(), "Second consumer thread should not fail");
        assertNotNull(processedOrderResultEventRepository.findAll().stream()
            .filter(e -> e.getEventId().equals(eventId))
            .findFirst()
            .orElse(null));
    }
}
