package com.orderflow.order.domain;

import jakarta.persistence.*;
import lombok.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;

@Entity
@Table(name = "tb_orders", schema = "orders")
@NoArgsConstructor
@Getter
@Setter
public class Order {
  private static final Logger log = LoggerFactory.getLogger(Order.class);

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Version
  @Column(nullable = false)
  private Long version;

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
    if (this.status == OrderStatus.CONFIRMED) {
      log.warn("Order {} already confirmed; ignoring duplicate confirmation", this.id);
      return;
    }
    if (this.status == OrderStatus.REJECTED) {
      log.warn("Order {} is rejected and cannot be confirmed; ignoring", this.id);
      return;
    }
    this.status = OrderStatus.CONFIRMED;
  }

  public void reject() {
    if (this.status == OrderStatus.REJECTED) {
      log.warn("Order {} already rejected; ignoring duplicate rejection", this.id);
      return;
    }
    if (this.status == OrderStatus.CONFIRMED) {
      log.warn("Order {} is confirmed and cannot be rejected; ignoring", this.id);
      return;
    }
    this.status = OrderStatus.REJECTED;
  }

}
