package com.orderflow.inventory.repository;

import com.orderflow.inventory.domain.ProcessedPaymentCompensationEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

public interface ProcessedPaymentCompensationEventRepository
        extends JpaRepository<ProcessedPaymentCompensationEvent, Long> {

    @Modifying
    @Transactional
    @Query(value = "INSERT INTO inventory.tb_processed_payment_compensation_events (event_id, processed_at) VALUES (:eventId, now()) ON CONFLICT (event_id) DO NOTHING",
            nativeQuery = true)
    int insertIfNotExists(@Param("eventId") UUID eventId);
}
