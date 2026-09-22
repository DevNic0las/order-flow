package com.orderflow.inventory.domain;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "tb_processed_inventory_events", schema = "inventory")
@NoArgsConstructor
@Getter
@Setter
public class ProcessedInventoryEvent {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "event_id", nullable = false, unique = true)
  private UUID eventId;

  @Column(name = "processed_at", nullable = false)
  private Instant processedAt;

  public ProcessedInventoryEvent(UUID eventId) {
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