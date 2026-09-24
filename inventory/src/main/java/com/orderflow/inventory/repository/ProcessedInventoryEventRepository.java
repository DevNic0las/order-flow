package com.orderflow.inventory.repository;

import com.orderflow.inventory.domain.ProcessedInventoryEvent;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

public interface ProcessedInventoryEventRepository extends JpaRepository<ProcessedInventoryEvent, Long> {
    boolean existsByEventId(java.util.UUID eventId);

    @Modifying
    @Transactional
    @Query(value = "INSERT INTO inventory.tb_processed_inventory_events (event_id, processed_at) VALUES (:eventId, now()) ON CONFLICT (event_id) DO NOTHING",
            nativeQuery = true)
    int insertIfNotExists(@Param("eventId") UUID eventId);
}
