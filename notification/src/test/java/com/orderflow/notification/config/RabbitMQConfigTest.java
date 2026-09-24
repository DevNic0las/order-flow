package com.orderflow.notification.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.FanoutExchange;
import org.springframework.amqp.core.Queue;

import static org.junit.jupiter.api.Assertions.*;

class RabbitMQConfigTest {

    private final RabbitMQConfig config = new RabbitMQConfig();

    @Test
    @DisplayName("Should configure queues with correct names and DLQ arguments")
    void shouldConfigureQueuesWithDlq() {
        Queue notificationQueue = config.notificationQueue();
        assertNotNull(notificationQueue);
        assertEquals("notification.queue", notificationQueue.getName());
        assertTrue(notificationQueue.isDurable());
        assertEquals("dlq.exchange", notificationQueue.getArguments().get("x-dead-letter-exchange"));
        assertEquals("notification.dlq", notificationQueue.getArguments().get("x-dead-letter-routing-key"));

        Queue emailVerificationQueue = config.emailVerificationQueue();
        assertNotNull(emailVerificationQueue);
        assertEquals("email.verification.queue", emailVerificationQueue.getName());
        assertTrue(emailVerificationQueue.isDurable());
        assertEquals("dlq.exchange", emailVerificationQueue.getArguments().get("x-dead-letter-exchange"));
        assertEquals("email.verification.dlq", emailVerificationQueue.getArguments().get("x-dead-letter-routing-key"));

        Queue dlq = config.notificationDlq();
        assertNotNull(dlq);
        assertEquals("notification.dlq", dlq.getName());
        assertTrue(dlq.isDurable());

        Queue emailVerificationDlq = config.emailVerificationDlq();
        assertNotNull(emailVerificationDlq);
        assertEquals("email.verification.dlq", emailVerificationDlq.getName());
        assertTrue(emailVerificationDlq.isDurable());
    }

    @Test
    @DisplayName("Should configure exchanges correctly")
    void shouldConfigureExchanges() {
        FanoutExchange orderResultExchange = config.orderResultExchange();
        assertNotNull(orderResultExchange);
        assertEquals("order.result.exchange", orderResultExchange.getName());

        DirectExchange notificationExchange = config.notificationExchange();
        assertNotNull(notificationExchange);
        assertEquals("notification.exchange", notificationExchange.getName());

        DirectExchange dlqExchange = config.dlqExchange();
        assertNotNull(dlqExchange);
        assertEquals("dlq.exchange", dlqExchange.getName());
    }

    @Test
    @DisplayName("Should configure bindings correctly")
    void shouldConfigureBindings() {
        Binding notifBinding = config.notificationBinding();
        assertNotNull(notifBinding);
        assertEquals("notification.queue", notifBinding.getDestination());
        assertEquals("order.result.exchange", notifBinding.getExchange());

        Binding emailBinding = config.emailVerificationBinding();
        assertNotNull(emailBinding);
        assertEquals("email.verification.queue", emailBinding.getDestination());
        assertEquals("notification.exchange", emailBinding.getExchange());
        assertEquals("rk.email.verification", emailBinding.getRoutingKey());

        Binding dlqBinding = config.notificationDlqBinding();
        assertNotNull(dlqBinding);
        assertEquals("notification.dlq", dlqBinding.getDestination());
        assertEquals("dlq.exchange", dlqBinding.getExchange());
        assertEquals("notification.dlq", dlqBinding.getRoutingKey());

        Binding emailVerificationDlqBinding = config.emailVerificationDlqBinding();
        assertNotNull(emailVerificationDlqBinding);
        assertEquals("email.verification.dlq", emailVerificationDlqBinding.getDestination());
        assertEquals("dlq.exchange", emailVerificationDlqBinding.getExchange());
        assertEquals("email.verification.dlq", emailVerificationDlqBinding.getRoutingKey());
    }
}
