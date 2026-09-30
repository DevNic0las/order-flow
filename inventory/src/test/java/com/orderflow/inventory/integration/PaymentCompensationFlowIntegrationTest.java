package com.orderflow.inventory.integration;

import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.*;

import com.orderflow.inventory.config.RabbitMQConfig;
import com.orderflow.inventory.domain.Inventory;
import com.orderflow.inventory.dto.PaymentCompensationEventDto;
import com.orderflow.inventory.repository.InventoryRepository;
import com.orderflow.inventory.repository.ProcessedPaymentCompensationEventRepository;
import java.time.Duration;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

/**
 * Contrato do lado do inventory para a compensação do payment.
 *
 * <p>Representa o passo "payment recusa → compensation → inventory devolve
 * estoque" publicando na {@code payment.compensation.exchange} exatamente o
 * payload que o payment publica ({@link PaymentCompensationEventDto}) e
 * verificando que o consumer real do inventory devolve o estoque de forma
 * idempotente. Sem mock entre serviços: usa o RabbitMQ real.
 */
@SpringBootTest
@Testcontainers
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class PaymentCompensationFlowIntegrationTest {

  @Container
  static PostgreSQLContainer<?> postgres =
      new PostgreSQLContainer<>("postgres:16")
          .withDatabaseName("inventory_test")
          .withUsername("test")
          .withPassword("test");

  @Container
  static GenericContainer<?> rabbit =
      new GenericContainer<>(DockerImageName.parse("rabbitmq:3.13-management-alpine"))
          .withExposedPorts(5672)
          .waitingFor(Wait.forLogMessage(".*Server startup complete.*", 1))
          .withStartupTimeout(Duration.ofMinutes(2));

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
    registry.add("spring.rabbitmq.host", rabbit::getHost);
    registry.add("spring.rabbitmq.port", () -> rabbit.getMappedPort(5672));
    registry.add("spring.rabbitmq.username", () -> "guest");
    registry.add("spring.rabbitmq.password", () -> "guest");
    registry.add("jwt.secret", () -> "integration-test-secret-key-32bytes-min");
  }

  @Autowired
  private RabbitTemplate rabbitTemplate;

  @Autowired
  private InventoryRepository inventoryRepository;

  @Autowired
  private ProcessedPaymentCompensationEventRepository processedCompensationRepository;

  private Long productId;

  @AfterEach
  void cleanup() {
    if (productId != null) {
      inventoryRepository.deleteById(productId);
      productId = null;
    }
    processedCompensationRepository.deleteAll();
  }

  @Test
  void compensationEvent_returnsReservedStock_once() {
    Inventory inventory = new Inventory();
    inventory.setProductName("CompensationProduct");
    inventory.setQuantity(10);
    productId = inventoryRepository.saveAndFlush(inventory).getId();

    UUID eventId = UUID.randomUUID();
    PaymentCompensationEventDto compensation =
        new PaymentCompensationEventDto(99L, productId, 5, eventId);

    rabbitTemplate.convertAndSend(
        RabbitMQConfig.PAYMENT_COMPENSATION_EXCHANGE, "", compensation);

    await().atMost(Duration.ofSeconds(10)).until(() ->
        inventoryRepository.findById(productId).orElseThrow().getQuantity() == 15);

    assertEquals(1, processedCompensationRepository.findAll().size());
    assertEquals(eventId, processedCompensationRepository.findAll().get(0).getEventId());
  }

  @Test
  void duplicateCompensationEvent_doesNotRestoreStockTwice() {
    Inventory inventory = new Inventory();
    inventory.setProductName("CompensationDuplicate");
    inventory.setQuantity(10);
    productId = inventoryRepository.saveAndFlush(inventory).getId();

    UUID eventId = UUID.randomUUID();
    PaymentCompensationEventDto compensation =
        new PaymentCompensationEventDto(99L, productId, 5, eventId);

    rabbitTemplate.convertAndSend(
        RabbitMQConfig.PAYMENT_COMPENSATION_EXCHANGE, "", compensation);
    await().atMost(Duration.ofSeconds(10)).until(() ->
        inventoryRepository.findById(productId).orElseThrow().getQuantity() == 15);

    // mesma eventId: não deve somar de novo
    rabbitTemplate.convertAndSend(
        RabbitMQConfig.PAYMENT_COMPENSATION_EXCHANGE, "", compensation);

    await().during(Duration.ofSeconds(2)).atMost(Duration.ofSeconds(3)).until(() ->
        inventoryRepository.findById(productId).orElseThrow().getQuantity() == 15);
    assertEquals(15, inventoryRepository.findById(productId).orElseThrow().getQuantity());
    assertEquals(1, processedCompensationRepository.findAll().size());
  }
}
