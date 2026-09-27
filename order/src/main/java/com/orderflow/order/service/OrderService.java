package com.orderflow.order.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.orderflow.order.domain.Order;
import com.orderflow.order.dtos.OrderEventDto;
import com.orderflow.order.dtos.OrderRequestDto;
import com.orderflow.order.dtos.OrderResponseDto;
import com.orderflow.order.exception.OrderNotFoundException;
import com.orderflow.order.messaging.OrderPublisher;
import com.orderflow.order.outbox.OutboxEvent;
import com.orderflow.order.outbox.OutboxEventStatus;
import com.orderflow.order.outbox.OutboxEventRepository;
import com.orderflow.order.repository.OrderIdempotencyKeyRepository;
import com.orderflow.order.repository.OrderRepository;
import com.orderflow.order.repository.ProcessedOrderResultEventRepository;
import com.orderflow.order.service.mapper.OrderMapper;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class OrderService {

  private static final Logger log = LoggerFactory.getLogger(OrderService.class);

  private final OrderPublisher orderPublisher;
  private final OrderMapper orderMapper;
  private final OrderRepository orderRepository;
  private final OutboxEventRepository outboxEventRepository;
  private final ObjectMapper objectMapper;
  private final ProcessedOrderResultEventRepository processedOrderResultEventRepository;
  private final OrderIdempotencyKeyRepository orderIdempotencyKeyRepository;

  @Transactional
  public OrderResponseDto createOrder(OrderRequestDto orderRequestDto, String userId){
    return createOrder(orderRequestDto, userId, null);
  }

  @Transactional
  public OrderResponseDto createOrder(OrderRequestDto orderRequestDto, String userId, String idempotencyKey){
    log.info("Creating order for productId={} with quantity={}", orderRequestDto.productId(), orderRequestDto.quantity());

    if (StringUtils.hasText(idempotencyKey)) {
      Optional<Long> existingOrderId = orderIdempotencyKeyRepository.findOrderIdByIdempotencyKey(idempotencyKey);
      if (existingOrderId.isPresent()) {
        Long orderId = existingOrderId.get();
        Order existingOrder = orderRepository.findById(orderId)
            .orElseThrow(() -> new IllegalStateException("Order not found for idempotency key: " + idempotencyKey));
        log.info("Returning existing order for idempotency key. orderId={}, key={}", orderId, idempotencyKey);
        return orderMapper.toResponseDto(existingOrder);
      }
    }

    Order order = new Order();
    order.setCustomerName(userId);
    order.setProductId(orderRequestDto.productId());
    order.setQuantity(orderRequestDto.quantity());
    UUID eventId = UUID.randomUUID();
    Order savedOrder = orderRepository.save(order);

    OrderEventDto orderEventDto = new OrderEventDto(
            eventId,
            savedOrder.getId(),
            savedOrder.getProductId(),
            savedOrder.getQuantity(),
            userId
    );

    String payload;
    try {
      payload = objectMapper.writeValueAsString(orderEventDto);
    } catch (JsonProcessingException e) {
      throw new IllegalStateException("Failed to serialize order event into outbox payload", e);
    }

    OutboxEvent outboxEvent = new OutboxEvent();
    outboxEvent.setEventId(eventId);
    outboxEvent.setEventType("ORDER_CREATED");
    outboxEvent.setPayload(payload);
    outboxEvent.setStatus(OutboxEventStatus.PENDING);
    outboxEvent.setCreatedAt(LocalDateTime.now());
    outboxEventRepository.save(outboxEvent);

    if (StringUtils.hasText(idempotencyKey)) {
      int inserted = orderIdempotencyKeyRepository.insertIfNotExists(idempotencyKey, savedOrder.getId());
      if (inserted == 0) {
        Long existingOrderId = orderIdempotencyKeyRepository.findOrderIdByIdempotencyKey(idempotencyKey)
            .orElseThrow(() -> new IllegalStateException("Concurrent create failed and idempotency key was not persisted for key: " + idempotencyKey));
        Order concurrentOrder = orderRepository.findById(existingOrderId)
            .orElseThrow(() -> new IllegalStateException("Concurrent order not found for idempotency key: " + idempotencyKey));
        log.info("Another request created the order first. Returning concurrent order. orderId={}, key={}", existingOrderId, idempotencyKey);
        return orderMapper.toResponseDto(concurrentOrder);
      }
    }

    return orderMapper.toResponseDto(savedOrder);
  }

  @Transactional
  public void processOrderResult(UUID eventId, Long orderId, boolean approved) {

    int inserted = processedOrderResultEventRepository
            .insertIfNotExists(eventId);

    if (inserted == 0) {
      log.info("Event already processed, ignoring. eventId={}", eventId);
      return;
    }

    if (approved) {
      confirmOrder(orderId);
    } else {
      rejectOrder(orderId);
    }
  }

  @Transactional
  public List<OrderResponseDto> getAllOrders() {
    return orderRepository.findAll().stream()
            .map(orderMapper::toResponseDto)
            .toList();
  }

  @Transactional
  public void confirmOrder(Long orderId){
    log.info("Confirming order with orderId={}", orderId);
    Order order = orderRepository.findById(orderId).orElseThrow(() -> new OrderNotFoundException("Order not found"));
    order.confirmed();
    orderRepository.save(order);
  }

  @Transactional
  public void rejectOrder(Long orderId){
    log.info("Rejecting order with orderId={}", orderId);
    Order order = orderRepository.findById(orderId).orElseThrow(() -> new OrderNotFoundException("Order not found"));
    order.reject();
    orderRepository.save(order);
  }
}
