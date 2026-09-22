package com.orderflow.order.repository;

import com.orderflow.order.domain.ProcessedOrderResultEvent;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

public interface ProcessedOrderResultEventRepository extends JpaRepository<ProcessedOrderResultEvent, Long> {

    @Modifying
    @Transactional
    @Query(value = "INSERT INTO orders.tb_processed_order_result_events (event_id, processed_at) VALUES (:eventId, now()) ON CONFLICT (event_id) DO NOTHING",
            nativeQuery = true)
    int insertIfNotExists(@Param("eventId") UUID eventId);
}
