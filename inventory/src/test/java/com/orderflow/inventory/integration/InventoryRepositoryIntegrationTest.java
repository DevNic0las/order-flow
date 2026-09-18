package com.orderflow.inventory.integration;

import com.orderflow.inventory.domain.Inventory;
import com.orderflow.inventory.repository.InventoryRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.OptimisticLockException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
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
class InventoryRepositoryIntegrationTest {

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
  }

  @Autowired
  private InventoryRepository inventoryRepository;
  @Autowired
  private TransactionTemplate transactionTemplate;
  @Autowired
  private EntityManagerFactory entityManagerFactory;



  @Test
  void shouldConnectToPostgres() {
    assertNotNull(inventoryRepository);
  }

  @Test
  void shouldSaveInventory() {
    Inventory inventory = new Inventory();
    inventory.setProductName("Produto Teste");
    inventory.setQuantity(10);

    Inventory saved = inventoryRepository.save(inventory);

    assertNotNull(saved.getId());
    assertNotNull(saved.getVersion());
  }
  @Test
  void shouldIncrementVersionWhenUpdatingInventory() {
    Inventory inventory = new Inventory();
    inventory.setProductName("Produto Teste");
    inventory.setQuantity(10);

    Inventory saved = inventoryRepository.saveAndFlush(inventory);

    Integer initialVersion = saved.getVersion();

    saved.setQuantity(20);

    Inventory updated = inventoryRepository.saveAndFlush(saved);

    assertNotNull(initialVersion);
    assertNotNull(updated.getVersion());

    assertEquals(initialVersion + 1, updated.getVersion());
  }

  @Test
  void shouldPreventConcurrentUpdatesWithOptimisticLocking() throws InterruptedException {

    Inventory inventory = new Inventory();
    inventory.setProductName("Produto Concorrente");
    inventory.setQuantity(10);

    Long inventoryId = transactionTemplate.execute(status ->
            inventoryRepository.saveAndFlush(inventory).getId());

    EntityManager emA = entityManagerFactory.createEntityManager();
    EntityManager emB = entityManagerFactory.createEntityManager();

    CountDownLatch bothRead = new CountDownLatch(2);
    CountDownLatch allowUpdate = new CountDownLatch(1);
    AtomicReference<Exception> exceptionFromB = new AtomicReference<>();

    Thread threadA = new Thread(() -> {
      emA.getTransaction().begin();
      Inventory invA = emA.find(Inventory.class, inventoryId);
      assertEquals(0, invA.getVersion());

      bothRead.countDown();
      await(bothRead);
      allowUpdate.countDown();

      invA.setQuantity(8);
      emA.flush();
      emA.getTransaction().commit(); // só aqui a linha fica livre pro B
    });

    Thread threadB = new Thread(() -> {
      emB.getTransaction().begin();
      Inventory invB = emB.find(Inventory.class, inventoryId);
      assertEquals(0, invB.getVersion());

      bothRead.countDown();
      await(bothRead);
      await(allowUpdate);

      invB.setQuantity(5);
      try {
        emB.flush();
        emB.getTransaction().commit();
      } catch (Exception e) {
        exceptionFromB.set(e);
        emB.getTransaction().rollback();
      }
    });

    threadA.start();
    threadB.start();
    threadA.join();
    threadB.join();

    emA.close();
    emB.close();

    assertNotNull(exceptionFromB.get());
    assertInstanceOf(OptimisticLockException.class, exceptionFromB.get()); // opcional, mais preciso
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