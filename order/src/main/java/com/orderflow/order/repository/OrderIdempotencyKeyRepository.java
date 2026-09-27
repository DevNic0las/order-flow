package com.orderflow.order.repository;

import com.orderflow.order.domain.OrderIdempotencyKey;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Repository
public interface OrderIdempotencyKeyRepository extends JpaRepository<OrderIdempotencyKey, Long> {

    @Query("select o.orderId from OrderIdempotencyKey o where o.idempotencyKey = :key")
    Optional<Long> findOrderIdByIdempotencyKey(@Param("key") String key);

    @Modifying
    @Transactional
    @Query(value = "INSERT INTO orders.tb_order_idempotency_keys (idempotency_key, order_id, created_at) VALUES (:key, :orderId, now()) ON CONFLICT (idempotency_key) DO NOTHING",
            nativeQuery = true)
    int insertIfNotExists(@Param("key") String key, @Param("orderId") Long orderId);
}
