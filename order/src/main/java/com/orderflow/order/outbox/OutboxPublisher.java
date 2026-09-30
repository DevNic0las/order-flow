package com.orderflow.order.outbox;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.orderflow.order.dtos.OrderEventDto;
import com.orderflow.order.messaging.OrderPublisher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Component
public class OutboxPublisher {
    private static final Logger log = LoggerFactory.getLogger(OutboxPublisher.class);

    private final OutboxEventRepository outboxEventRepository;
    private final OrderPublisher orderPublisher;
    private final ObjectMapper objectMapper;

    public OutboxPublisher(OutboxEventRepository outboxEventRepository, OrderPublisher orderPublisher, ObjectMapper objectMapper) {
        this.outboxEventRepository = outboxEventRepository;
        this.orderPublisher = orderPublisher;
        this.objectMapper = objectMapper;
    }

    @Scheduled(fixedDelay = 5000)
    public void publishPendingEvents() {
        List<OutboxEvent> events = outboxEventRepository.findPendingEvents(OutboxEventStatus.PENDING, PageRequest.of(0, 20));

        for (OutboxEvent event : events) {
            try {
                OrderEventDto dto = objectMapper.readValue(event.getPayload(), OrderEventDto.class);
                orderPublisher.publishOrder(dto);
                markPublished(event.getId());
            } catch (Exception ex) {
                log.warn("Failed to publish outbox event id={}, eventId={}, eventType={}. Keeping for retry.",
                        event.getId(), event.getEventId(), event.getEventType(), ex);
            }
        }
    }

    @Transactional
    public void markPublished(Long outboxId) {
        outboxEventRepository.markStatus(outboxId, OutboxEventStatus.PUBLISHED, LocalDateTime.now());
    }
}
