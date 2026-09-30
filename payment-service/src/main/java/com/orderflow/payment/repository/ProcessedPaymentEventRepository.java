package com.orderflow.payment.repository;

import com.orderflow.payment.domain.ProcessedPaymentEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

public interface ProcessedPaymentEventRepository extends JpaRepository<ProcessedPaymentEvent, Long> {

    @Modifying
    @Transactional
    @Query(value = "INSERT INTO payment.tb_processed_payment_events (event_id, processed_at) VALUES (:eventId, now()) ON CONFLICT (event_id) DO NOTHING",
            nativeQuery = true)
    int insertIfNotExists(@Param("eventId") UUID eventId);
}
