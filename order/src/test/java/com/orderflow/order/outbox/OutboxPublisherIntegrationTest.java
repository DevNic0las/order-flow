package com.orderflow.order.outbox;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import com.orderflow.order.dtos.OrderEventDto;
import com.orderflow.order.dtos.OrderRequestDto;
import com.orderflow.order.messaging.OrderPublisher;
import com.orderflow.order.repository.OrderRepository;
import com.orderflow.order.service.OrderService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.junit.jupiter.Container;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;

@SpringBootTest(properties = {
        "spring.rabbitmq.listener.simple.auto-startup=false",
        "spring.rabbitmq.listener.direct.auto-startup=false"
})
@Testcontainers
class OutboxPublisherIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.datasource.driver-class-name", () -> "org.postgresql.Driver");
        registry.add("spring.flyway.url", postgres::getJdbcUrl);
        registry.add("spring.flyway.user", postgres::getUsername);
        registry.add("spring.flyway.password", postgres::getPassword);
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "validate");
    }

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
        OrderRequestDto request = new OrderRequestDto("Ana", 42L, 3);

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
