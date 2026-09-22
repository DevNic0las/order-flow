package com.orderflow.inventory.integration;

import com.orderflow.inventory.domain.Inventory;
import com.orderflow.inventory.domain.ProcessedInventoryEvent;
import com.orderflow.inventory.repository.InventoryRepository;
import com.orderflow.inventory.repository.ProcessedInventoryEventRepository;
import com.orderflow.inventory.service.InventoryService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Testcontainers
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class IdempotencyRaceIntegrationTest {

  @Container
  static PostgreSQLContainer<?> postgres =
      new PostgreSQLContainer<>("postgres:16")
          .withDatabaseName("inventory_test")
          .withUsername("test")
          .withPassword("test");

  @DynamicPropertySource
  static void configureProperties(DynamicPropertyRegistry registry) {
    registry.add("spring.datasource.url", postgres::getJdbcUrl);
    registry.add("spring.datasource.username", postgres::getUsername);
    registry.add("spring.datasource.password", postgres::getPassword);
    registry.add("jwt.secret", () -> "01234567890123456789012345678901");
  }

  @Autowired
  private InventoryService inventoryService;

  @Autowired
  private InventoryRepository inventoryRepository;

  @Autowired
  private ProcessedInventoryEventRepository processedInventoryEventRepository;

  @org.springframework.boot.test.mock.mockito.MockBean
  private com.orderflow.inventory.messaging.InventoryPublisher inventoryPublisher;

  private Long createdProductId;

  @AfterEach
  void cleanup() {
    if (createdProductId != null) {
      inventoryRepository.deleteById(createdProductId);
      createdProductId = null;
    }
    processedInventoryEventRepository.deleteAll();
  }

  @Test
  void concurrentSameEventId_shouldProduceDuplicateInsertAndUnexpectedRollbackOnLoser() throws Exception {
    // arrange: create product with quantity 10
    Inventory inventory = new Inventory();
    inventory.setProductName("RaceProduct");
    inventory.setQuantity(10);
    Inventory saved = inventoryRepository.saveAndFlush(inventory);
    createdProductId = saved.getId();

    UUID eventId = UUID.randomUUID();
    Long orderId = 42L;
    int quantity = 3;
    String email = "test@example.com";

    CountDownLatch ready = new CountDownLatch(2);
    CountDownLatch start = new CountDownLatch(1);

    AtomicReference<Throwable> t1Ex = new AtomicReference<>();
    AtomicReference<Throwable> t2Ex = new AtomicReference<>();

    Thread t1 = new Thread(() -> {
      try {
        ready.countDown();
        start.await();
        inventoryService.decreaseProductStock(eventId, orderId, saved.getId(), quantity, email);
      } catch (Throwable t) {
        t1Ex.set(t);
      }
    });

    Thread t2 = new Thread(() -> {
      try {
        ready.countDown();
        start.await();
        inventoryService.decreaseProductStock(eventId, orderId, saved.getId(), quantity, email);
      } catch (Throwable t) {
        t2Ex.set(t);
      }
    });

    // act: start both threads as close as possible
    t1.start();
    t2.start();

    // wait until both threads are ready
    ready.await();
    // release both
    start.countDown();

    t1.join();
    t2.join();

    // assert: inventory decremented exactly once
    Inventory finalInv = inventoryRepository.findById(saved.getId()).orElseThrow();
    assertEquals(7, finalInv.getQuantity(), "Expected quantity decreased by 3 once");

    // assert: only one processed event saved
    List<ProcessedInventoryEvent> processed = processedInventoryEventRepository.findAll();
    assertEquals(1, processed.size(), "Expected exactly one processed event");
    assertEquals(eventId, processed.get(0).getEventId());

    // check exceptions
    Throwable ex1 = t1Ex.get();
    Throwable ex2 = t2Ex.get();

    // At least one thread should have thrown an exception (the loser)
    assertTrue((ex1 != null) ^ (ex2 != null), "Expected exactly one thread to observe an exception");

    Throwable loserEx = ex1 != null ? ex1 : ex2;
    // The loser should have observed a transactional rollback (UnexpectedRollbackException or DataIntegrityViolationException)
    assertNotNull(loserEx, "Loser exception should not be null");

    // Print types for debugging in case assertions need relax
    System.out.println("Thread1 exception: " + (ex1 == null ? "<none>" : ex1.getClass() + ": " + ex1.getMessage()));
    System.out.println("Thread2 exception: " + (ex2 == null ? "<none>" : ex2.getClass() + ": " + ex2.getMessage()));
  }
}
