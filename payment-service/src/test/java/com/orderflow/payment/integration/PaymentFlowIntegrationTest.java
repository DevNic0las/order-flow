package com.orderflow.payment.integration;

import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.*;

import com.orderflow.payment.config.RabbitMQConfig;
import com.orderflow.payment.domain.ProcessedPaymentEvent;
import com.orderflow.payment.dto.PaymentCompensationEventDto;
import com.orderflow.payment.dto.PaymentEventDto;
import com.orderflow.payment.dto.PaymentResultEventDto;
import com.orderflow.payment.repository.ProcessedPaymentEventRepository;
import java.time.Duration;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.MessageConverter;
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
 * Integração ponta a ponta do payment através de um RabbitMQ e Postgres reais.
 *
 * <p>Usa {@link GenericContainer} (não {@code RabbitMQContainer}) para evitar o
 * conflito entre o módulo testcontainers-rabbitmq 1.17.6 e o core 2.0.5 fixado
 * pelo BOM. A fila de resultado ({@code order.result.queue}) não é declarada
 * pelo payment, então é criada aqui como observer para capturar o que o serviço
 * publica no fanout {@code order.result.exchange}.
 */
@SpringBootTest
@Testcontainers
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class PaymentFlowIntegrationTest {

  @Container
  static PostgreSQLContainer<?> postgres =
      new PostgreSQLContainer<>("postgres:16")
          .withDatabaseName("payment_test")
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
  private ProcessedPaymentEventRepository processedPaymentEventRepository;

  @Autowired
  private org.springframework.amqp.core.AmqpAdmin amqpAdmin;

  @AfterEach
  void cleanup() {
    processedPaymentEventRepository.deleteAll();
  }

  // ─────────────────── consumo + resultado aprovado ───────────────────

  @Test
  void paymentRequest_isConsumed_processed_persisted_andResultPublished() {
    UUID eventId = UUID.randomUUID();

    // observer: cria uma fila temporária ligada ao fanout de resultado e
    // assina para receber tudo que o payment publicar.
    String observerQueue = "test.order.result." + UUID.randomUUID();
    declareQueueBoundToFanout(observerQueue, RabbitMQConfig.ORDER_RESULT_EXCHANGE);

    PaymentEventDto event = new PaymentEventDto(42L, 7L, 2, "buyer@example.com", eventId);
    rabbitTemplate.convertAndSend(RabbitMQConfig.PAYMENT_EXCHANGE, RabbitMQConfig.RK_PAYMENT, event);

    // 1) processamento persistiu a idempotência
    await().atMost(Duration.ofSeconds(10)).until(() ->
        processedPaymentEventRepository.findAll().size() == 1);
    List<ProcessedPaymentEvent> processed = processedPaymentEventRepository.findAll();
    assertEquals(eventId, processed.get(0).getEventId());

    // 2) publicou o resultado aprovado no fanout order.result.exchange
    PaymentResultEventDto result = awaitResult(observerQueue);
    assertNotNull(result, "Expected a PaymentResultEventDto on order.result.exchange");
    assertEquals(42L, result.orderId());
    assertTrue(result.approved());
    assertEquals(eventId, result.eventId(), "eventId must be preserved end-to-end");
  }

  // ─────────────────────────── duplicado ───────────────────────────

  @Test
  void duplicateEvent_isProcessedOnlyOnce() {
    UUID eventId = UUID.randomUUID();
    String observerQueue = "test.order.result.dup." + UUID.randomUUID();
    declareQueueBoundToFanout(observerQueue, RabbitMQConfig.ORDER_RESULT_EXCHANGE);

    PaymentEventDto event = new PaymentEventDto(43L, 7L, 1, "buyer@example.com", eventId);
    rabbitTemplate.convertAndSend(RabbitMQConfig.PAYMENT_EXCHANGE, RabbitMQConfig.RK_PAYMENT, event);
    await().atMost(Duration.ofSeconds(10)).until(() ->
        processedPaymentEventRepository.findAll().size() == 1);

    // mesma eventId de novo
    rabbitTemplate.convertAndSend(RabbitMQConfig.PAYMENT_EXCHANGE, RabbitMQConfig.RK_PAYMENT, event);

    // continua havendo exatamente um registro; e apenas um resultado publicado
    await().during(Duration.ofSeconds(2)).atMost(Duration.ofSeconds(3)).until(() ->
        processedPaymentEventRepository.findAll().size() == 1);
    assertEquals(1, processedPaymentEventRepository.findAll().size());

    PaymentResultEventDto first = awaitResult(observerQueue);
    assertNotNull(first);
    // nenhuma segunda mensagem no observer
    Message extra = rabbitTemplate.receive(observerQueue, 2000);
    assertNull(extra, "Duplicate event must not publish a second result");
  }

  // ─────────────────────── inválido/falha → DLQ ───────────────────────

  @Test
  void invalidMessage_isRoutedToDlq() {
    // listener configurado para não reenfileirar; o consumer falha ao
    // desserializar (corpo não é JSON válido) → vai para payment.dlq.
    rabbitTemplate.convertAndSend(
        RabbitMQConfig.PAYMENT_EXCHANGE,
        RabbitMQConfig.RK_PAYMENT,
        "not-a-valid-payment-event".getBytes());

    // a DLQ é declarada pelo próprio payment (payment.dlq) — drenamos até achar.
    await().atMost(Duration.ofSeconds(15)).untilAsserted(() -> {
      Message dead = rabbitTemplate.receive(RabbitMQConfig.PAYMENT_DLQ, 500);
      assertNotNull(dead, "Expected the invalid message to land in payment.dlq");
    });
  }

  // ─────────────────────────── helpers ───────────────────────────

  private void declareQueueBoundToFanout(String queueName, String exchangeName) {
    // autoDelete=false: a fila observer precisa sobreviver entre os dois
    // receives do teste de duplicado (o consumidor é cancelado a cada receive).
    org.springframework.amqp.core.Queue queue = new org.springframework.amqp.core.Queue(queueName, false, false, false);
    org.springframework.amqp.core.FanoutExchange exchange = new org.springframework.amqp.core.FanoutExchange(exchangeName);
    org.springframework.amqp.core.Binding binding =
        org.springframework.amqp.core.BindingBuilder.bind(queue).to(exchange);
    amqpAdmin.declareExchange(exchange);
    amqpAdmin.declareQueue(queue);
    amqpAdmin.declareBinding(binding);
  }

  private PaymentResultEventDto awaitResult(String observerQueue) {
    MessageConverter converter = rabbitTemplate.getMessageConverter();
    java.util.concurrent.atomic.AtomicReference<PaymentResultEventDto> ref =
        new java.util.concurrent.atomic.AtomicReference<>();
    await().atMost(Duration.ofSeconds(10)).until(() -> {
      Message message = rabbitTemplate.receive(observerQueue, 500);
      if (message == null) {
        return false;
      }
      Object payload = converter.fromMessage(message);
      if (payload instanceof PaymentResultEventDto dto) {
        ref.set(dto);
        return true;
      }
      return false;
    });
    return ref.get();
  }
}
