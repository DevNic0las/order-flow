package com.orderflow.inventory.domain;


import jakarta.persistence.*;

import java.time.Instant;
import java.time.OffsetDateTime;

@Entity
@Table(name="tb_inventory_idempotency_keys", schema = "inventory")
public class InventoryIdempotencyKey {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "idempotency_key", nullable = false, unique = true)
  private String idempotencyKey;

  @Column(name = "product_id", nullable = false)
  private Long productId;

  @Column(name = "created_at", nullable = false)
  private OffsetDateTime createdAt;


  @PrePersist
  void prePersist() {
    if (this.createdAt == null) {
      this.createdAt = OffsetDateTime.now();
    }
  }

}
