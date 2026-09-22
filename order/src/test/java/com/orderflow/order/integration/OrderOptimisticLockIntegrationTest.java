package com.orderflow.order.integration;

import com.orderflow.order.domain.Order;
import com.orderflow.order.domain.OrderStatus;
import com.orderflow.order.repository.OrderRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.OptimisticLockException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
@Testcontainers
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class OrderOptimisticLockIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16")
            .withDatabaseName("order_test")
            .withUsername("test")
            .withPassword("test");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.datasource.driver-class-name", () -> "org.postgresql.Driver");
        registry.add("spring.flyway.url", postgres::getJdbcUrl);
        registry.add("spring.flyway.user", postgres::getUsername);
        registry.add("spring.flyway.password", postgres::getPassword);
    }

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private TransactionTemplate transactionTemplate;

    @Autowired
    private EntityManagerFactory entityManagerFactory;

    private Long createdOrderId;

    @AfterEach
    void cleanup() {
        if (createdOrderId != null) {
            transactionTemplate.executeWithoutResult(status -> orderRepository.deleteById(createdOrderId));
            createdOrderId = null;
        }
    }

    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    void shouldDetectConcurrentStatusUpdateWithOptimisticLocking() throws InterruptedException {
        Order order = new Order();
        order.setCustomerName("Alice");
        order.setProductId(99L);
        order.setQuantity(3);
        order.setStatus(OrderStatus.PENDING);

        createdOrderId = transactionTemplate.execute(status -> orderRepository.saveAndFlush(order).getId());

        EntityManager emA = entityManagerFactory.createEntityManager();
        EntityManager emB = entityManagerFactory.createEntityManager();

        CountDownLatch bothRead = new CountDownLatch(2);
        AtomicReference<Exception> failureFromA = new AtomicReference<>();
        AtomicReference<Exception> failureFromB = new AtomicReference<>();

        Thread threadA = new Thread(() -> {
            emA.getTransaction().begin();
            Order orderA = emA.find(Order.class, createdOrderId);
            assertEquals(OrderStatus.PENDING, orderA.getStatus());
            assertEquals(0L, orderA.getVersion());

            bothRead.countDown();
            await(bothRead);

            orderA.setStatus(OrderStatus.CONFIRMED);
            try {
                emA.flush();
                emA.getTransaction().commit();
            } catch (Exception ex) {
                failureFromA.set(ex);
                emA.getTransaction().rollback();
            }
        });

        Thread threadB = new Thread(() -> {
            emB.getTransaction().begin();
            Order orderB = emB.find(Order.class, createdOrderId);
            assertEquals(OrderStatus.PENDING, orderB.getStatus());
            assertEquals(0L, orderB.getVersion());

            bothRead.countDown();
            await(bothRead);

            orderB.setStatus(OrderStatus.REJECTED);
            try {
                emB.flush();
                emB.getTransaction().commit();
            } catch (Exception ex) {
                failureFromB.set(ex);
                emB.getTransaction().rollback();
            }
        });

        threadA.start();
        threadB.start();
        threadA.join();
        threadB.join();

        emA.close();
        emB.close();

        boolean aFailed = failureFromA.get() != null;
        boolean bFailed = failureFromB.get() != null;
        assertTrue(aFailed ^ bFailed,
                "Exactly one concurrent update should fail with optimistic locking; aFailed=" + aFailed + " bFailed=" + bFailed);

        Exception failure = aFailed ? failureFromA.get() : failureFromB.get();
        assertTrue(failure instanceof OptimisticLockException || failure instanceof ObjectOptimisticLockingFailureException,
                "Expected optimistic locking failure, got: " + failure);

        Order result = orderRepository.findById(createdOrderId).orElseThrow();
        assertEquals(1L, result.getVersion(), "Version should be incremented exactly once");

        if (aFailed) {
            assertEquals(OrderStatus.REJECTED, result.getStatus(), "The winner must be the non-failing update");
        } else {
            assertEquals(OrderStatus.CONFIRMED, result.getStatus(), "The winner must be the non-failing update");
        }
    }

    private void await(CountDownLatch latch) {
        try {
            latch.await();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException(e);
        }
    }
}
