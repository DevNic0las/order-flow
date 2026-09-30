package com.orderflow.order;

import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;

/**
 * Base class for {@code order} integration tests.
 *
 * <p>Owns a single PostgreSQL Testcontainer for the whole test JVM
 * (the Testcontainers "singleton container" pattern) and registers the shared
 * Spring properties. Previously every {@code @SpringBootTest} class started its
 * own {@code @Container}, and because the Spring TestContext cache key is
 * derived from the context configuration — including the per-class
 * {@code @DynamicPropertySource} methods — a cached ApplicationContext could
 * keep a DataSource pointing at a container that had already been stopped after
 * a previous test class. That produced
 * "Connection is not available / Connection refused / Failed to load
 * ApplicationContext" in CI, where timing and resource pressure differ from a
 * developer machine. One container, started once and kept alive for the entire
 * JVM (Ryuk reaps it at exit), makes context reuse safe and avoids container
 * churn. The idempotency race test relies on the same live database.
 *
 * <p>Also centralizes:
 * <ul>
 *   <li>{@code order.scheduling.enabled=false} — prevents the {@code @Scheduled}
 *       Outbox publisher from running in the background; tests exercise
 *       {@code OutboxPublisher#publishPendingEvents()} directly.</li>
 *   <li>RabbitMQ listener auto-startup off — AMQP listeners must not connect
 *       during tests.</li>
 *   <li>{@code jwt.secret} — the order context needs it for
 *       {@code JwtTokenValidator}; without an explicit value the build only
 *       works when {@code JWT_SECRET} happens to be exported (true locally,
 *       not in CI).</li>
 * </ul>
 *
 * <p>Scheduling stays enabled in production via
 * {@link com.orderflow.order.config.SchedulingConfig} ({@code matchIfMissing = true}).
 */
@SpringBootTest(properties = {
        "order.scheduling.enabled=false",
        "spring.rabbitmq.listener.simple.auto-startup=false",
        "spring.rabbitmq.listener.direct.auto-startup=false",
        "spring.jpa.open-in-view=false",
        "spring.jpa.hibernate.ddl-auto=validate"
})
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
public abstract class IntegrationTestBase {

    static final PostgreSQLContainer<?> POSTGRES =
        new PostgreSQLContainer<>("postgres:16")
            .withDatabaseName("order_test")
            .withUsername("test")
            .withPassword("test");

    static {
        POSTGRES.start();
    }

    @DynamicPropertySource
    static void sharedProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
        registry.add("spring.datasource.driver-class-name", () -> "org.postgresql.Driver");
        registry.add("spring.flyway.url", POSTGRES::getJdbcUrl);
        registry.add("spring.flyway.user", POSTGRES::getUsername);
        registry.add("spring.flyway.password", POSTGRES::getPassword);
        registry.add("jwt.secret", () -> "test-secret");
    }
}
