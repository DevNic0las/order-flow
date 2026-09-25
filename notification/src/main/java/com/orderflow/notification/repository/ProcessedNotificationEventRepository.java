package com.orderflow.notification.repository;

import com.orderflow.notification.domain.ProcessedNotificationEvent;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

public interface ProcessedNotificationEventRepository extends JpaRepository<ProcessedNotificationEvent, Long> {
    boolean existsByEventId(UUID eventId);

    @Modifying
    @Transactional
    @Query(value = "INSERT INTO notification.tb_processed_notification_events (event_id, processed_at) VALUES (:eventId, now()) ON CONFLICT (event_id) DO NOTHING",
            nativeQuery = true)
    int insertIfNotExists(@Param("eventId") UUID eventId);
}
