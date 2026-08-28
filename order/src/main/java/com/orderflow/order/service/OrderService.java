package com.orderflow.order.service;

import com.orderflow.order.domain.Order;
import com.orderflow.order.dtos.OrderEventDto;
import com.orderflow.order.dtos.OrderRequestDto;
import com.orderflow.order.dtos.OrderResponseDto;
import com.orderflow.order.exception.OrderNotFoundException;
import com.orderflow.order.messaging.OrderPublisher;
import com.orderflow.order.repository.OrderRepository;
import com.orderflow.order.service.mapper.OrderMapper;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class OrderService {

  private final OrderPublisher orderPublisher;
  private final OrderMapper orderMapper;
  private final OrderRepository orderRepository;

  @Transactional
  public OrderResponseDto createOrder(OrderRequestDto orderRequestDto, String userId){
    log.info("Creating order for productId={} with quantity={}", orderRequestDto.productId(), orderRequestDto.quantity());

    Order order = new Order();
    order.setCustomerName(orderRequestDto.customerName());
    order.setProductId(orderRequestDto.productId());
    order.setQuantity(orderRequestDto.quantity());

    Order savedOrder = orderRepository.save(order);

    OrderEventDto orderEventDto = new OrderEventDto(
            order.getId(),
            order.getProductId(),
            order.getQuantity(),
            userId
    );

    orderPublisher.publishOrder(orderEventDto);

    return orderMapper.toResponseDto(savedOrder);
  }
  public void confirmOrder(Long orderId){
    log.info("Confirming order with orderId={}", orderId);
    Order order = orderRepository.findById(orderId).orElseThrow(() -> new OrderNotFoundException("Order not found"));
    order.confirmed();
    orderRepository.save(order);
  }
  public void rejectOrder(Long orderId){
    log.info("Rejecting order with orderId={}", orderId);
    Order order = orderRepository.findById(orderId).orElseThrow(() -> new OrderNotFoundException("Order not found"));
    order.reject();
    orderRepository.save(order);
  }
}
