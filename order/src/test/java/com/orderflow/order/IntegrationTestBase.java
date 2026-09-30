package com.orderflow.order;

import org.springframework.boot.test.context.SpringBootTest;

/**
 * Base class for {@code order} integration tests.
 *
 * <p>Centralizes the Spring test context configuration shared by every
 * integration test:
 *
 * <ul>
 *   <li>{@code order.scheduling.enabled=false} — keeps the {@code @Scheduled}
 *       Outbox publisher from running in the background. Without this, the
 *       scheduler wakes up after a test and queries a datasource whose
 *       Testcontainers PostgreSQL has already been shut down, causing
 *       "Connection is not available / Connection refused" and flaky context
 *       load failures (observed in CI). Tests that exercise the publisher do so
 *       by calling {@code OutboxPublisher#publishPendingEvents()} directly.</li>
 *   <li>RabbitMQ listener auto-startup is disabled so AMQP listeners do not
 *       try to connect to a broker during tests.</li>
 * </ul>
 *
 * <p>Scheduling remains enabled in production via
 * {@link com.orderflow.order.config.SchedulingConfig} ({@code matchIfMissing = true}).
 */
@SpringBootTest(properties = {
        "order.scheduling.enabled=false",
        "spring.rabbitmq.listener.simple.auto-startup=false",
        "spring.rabbitmq.listener.direct.auto-startup=false",
        "spring.jpa.open-in-view=false"
})
public abstract class IntegrationTestBase {
}
