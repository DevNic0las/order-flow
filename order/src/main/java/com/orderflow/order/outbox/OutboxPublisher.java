package com.orderflow.order.outbox;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.orderflow.order.dtos.OrderEventDto;
import com.orderflow.order.messaging.OrderPublisher;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class OutboxPublisher {

    private final OutboxEventRepository outboxEventRepository;
    private final OrderPublisher orderPublisher;
    private final ObjectMapper objectMapper;

    @Scheduled(fixedDelay = 1000)
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
