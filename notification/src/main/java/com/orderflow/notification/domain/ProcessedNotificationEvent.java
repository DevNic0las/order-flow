package com.orderflow.notification.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "tb_processed_notification_events", schema = "notification")
@NoArgsConstructor
@Getter
@Setter
public class ProcessedNotificationEvent {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "event_id", nullable = false, unique = true)
  private UUID eventId;

  @Column(name = "processed_at", nullable = false)
  private Instant processedAt;

  public ProcessedNotificationEvent(UUID eventId) {
     this.eventId = eventId;
     this.processedAt = Instant.now();
  }

  @PrePersist
  void prePersist() {
     if (this.processedAt == null) {
        this.processedAt = Instant.now();
     }
  }
}
