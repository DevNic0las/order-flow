package com.orderflow.order.domain;

import jakarta.persistence.*;
import lombok.*;


import java.time.LocalDateTime;

@Entity
@Table(name = "tb_orders", schema = "orders")
@NoArgsConstructor
@Getter
@Setter
public class Order {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name="customer_name", nullable = false)
  private String customerName;

  private Long productId;

  private Integer quantity;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 20)
  private OrderStatus status;

  private LocalDateTime createdAt;

  private LocalDateTime updatedAt;

  @PrePersist
  void prePersist(){
    this.createdAt = LocalDateTime.now();
    this.updatedAt = LocalDateTime.now();
    this.status = OrderStatus.PENDING;
  }

  @PreUpdate
  void preUpdate() {
    this.updatedAt = LocalDateTime.now();
  }

  public void confirmed(){
    this.status = OrderStatus.CONFIRMED;
  }

  public void reject() {
    this.status = OrderStatus.REJECTED;
  }

}
