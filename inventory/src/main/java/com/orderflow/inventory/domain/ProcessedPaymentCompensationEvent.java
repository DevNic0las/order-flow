package com.orderflow.inventory.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "tb_processed_payment_compensation_events", schema = "inventory")
@NoArgsConstructor
@Getter
@Setter
public class ProcessedPaymentCompensationEvent {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "event_id", nullable = false, unique = true)
  private UUID eventId;

  @Column(name = "processed_at", nullable = false)
  private Instant processedAt;

  public ProcessedPaymentCompensationEvent(UUID eventId) {
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
