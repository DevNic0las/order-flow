package com.overflow.inventory.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name="tb_inventory", schema = "inventory")
@Getter
@Setter
@NoArgsConstructor
public class Inventory {

  @Id
  @GeneratedValue(strategy = jakarta.persistence.GenerationType.IDENTITY)
  private Long id;

  private String productName;

  private Integer quantity;

  private LocalDateTime createdAt;

  private LocalDateTime updatedAt;

  @PrePersist
  public void prePersist() {
    this.createdAt = LocalDateTime.now();
    this.updatedAt = LocalDateTime.now();
  }


  public boolean withdraw(Integer quantity) {
    if(this.quantity < quantity) {
     return false;
    }
    this.quantity -= quantity;
    return true;
  }

}
