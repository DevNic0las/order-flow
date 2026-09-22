package com.orderflow.inventory.repository;

import com.orderflow.inventory.domain.ProcessedInventoryEvent;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProcessedInventoryEventRepository extends JpaRepository<ProcessedInventoryEvent, Long> {
    boolean existsByEventId(java.util.UUID eventId);
}
