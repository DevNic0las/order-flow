package com.orderflow.order.domain;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "tb_processed_order_result_events", schema = "orders")
public class ProcessedOrderResultEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "event_id", nullable = false, unique = true)
    private UUID eventId;

    @Column(name = "processed_at", nullable = false)
    private LocalDateTime processedAt;

    @PrePersist
    void prePersist() {
        this.processedAt = LocalDateTime.now();
    }

    public ProcessedOrderResultEvent() {
    }

    public ProcessedOrderResultEvent(UUID eventId) {
        this.eventId = eventId;
    }

    public Long getId() {
        return id;
    }

    public UUID getEventId() {
        return eventId;
    }

    public LocalDateTime getProcessedAt() {
        return processedAt;
    }
}
