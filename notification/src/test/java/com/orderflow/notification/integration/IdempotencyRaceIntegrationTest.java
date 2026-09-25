package com.orderflow.notification.integration;

import com.orderflow.notification.domain.ProcessedNotificationEvent;
import com.orderflow.notification.dto.NotificationEventDto;
import com.orderflow.notification.dto.NotificationEventEmailDto;
import com.orderflow.notification.port.EmailSender;
import com.orderflow.notification.repository.ProcessedNotificationEventRepository;
import com.orderflow.notification.service.NotificationService;
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
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@SpringBootTest
@Testcontainers
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class IdempotencyRaceIntegrationTest {

  @Container
  static PostgreSQLContainer<?> postgres =
      new PostgreSQLContainer<>("postgres:16")
          .withDatabaseName("notification_test")
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
  private NotificationService notificationService;

  @Autowired
  private ProcessedNotificationEventRepository processedNotificationEventRepository;

  @org.springframework.boot.test.mock.mockito.MockBean
  private EmailSender emailSender;

  @AfterEach
  void cleanup() {
    processedNotificationEventRepository.deleteAll();
  }

  @Test
  void concurrentSameEventId_shouldSendEmailExactlyOnce() throws Exception {
    UUID eventId = UUID.randomUUID();
    NotificationEventDto event = new NotificationEventDto(42L, true, "test@example.com", eventId);

    CountDownLatch ready = new CountDownLatch(2);
    CountDownLatch start = new CountDownLatch(1);

    AtomicReference<Throwable> t1Ex = new AtomicReference<>();
    AtomicReference<Throwable> t2Ex = new AtomicReference<>();

    Thread t1 = new Thread(() -> {
      try {
        ready.countDown();
        start.await();
        notificationService.processNotification(event);
      } catch (Throwable t) {
        t1Ex.set(t);
      }
    });

    Thread t2 = new Thread(() -> {
      try {
        ready.countDown();
        start.await();
        notificationService.processNotification(event);
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

    // assert: email sent exactly once
    verify(emailSender, times(1)).sendMessageEmail(org.mockito.ArgumentMatchers.any(NotificationEventEmailDto.class));

    // assert: only one processed event saved
    List<ProcessedNotificationEvent> processed = processedNotificationEventRepository.findAll();
    assertEquals(1, processed.size(), "Expected exactly one processed event");
    assertEquals(eventId, processed.get(0).getEventId());

    // check exceptions: with ON CONFLICT approach no thread should throw
    Throwable ex1 = t1Ex.get();
    Throwable ex2 = t2Ex.get();

    assertNull(ex1, "Thread1 should not have thrown");
    assertNull(ex2, "Thread2 should not have thrown");
  }
}
