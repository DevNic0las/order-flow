package com.orderflow.order.outbox;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

public interface OutboxEventRepository extends JpaRepository<OutboxEvent, Long> {

    @Query("select o from OutboxEvent o where o.status = :status order by o.createdAt asc")
    List<OutboxEvent> findPendingEvents(@Param("status") OutboxEventStatus status, Pageable pageable);

    @Modifying
    @Transactional
    @Query("update OutboxEvent o set o.status = :status, o.publishedAt = :publishedAt where o.id = :id")
    void markStatus(@Param("id") Long id, @Param("status") OutboxEventStatus status, @Param("publishedAt") LocalDateTime publishedAt);
}
