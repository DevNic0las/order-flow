package com.orderflow.inventory.repository;

import com.orderflow.inventory.domain.InventoryIdempotencyKey;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
@Repository
public interface InventoryIdempotencyKeyRepository extends JpaRepository<InventoryIdempotencyKey, Long> {

  @Query("select o.productId from InventoryIdempotencyKey o where o.idempotencyKey = :key")
  Optional<Long> findInventoryIdByIdempotencyKey(@Param("key") String key);
}
