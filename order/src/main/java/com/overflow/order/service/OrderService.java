package com.overflow.order.service;

import com.overflow.order.domain.Order;
import com.overflow.order.dtos.OrderRequestDto;
import com.overflow.order.dtos.OrderResponseDto;
import com.overflow.order.exception.OrderNotFoundException;
import com.overflow.order.messaging.OrderPublisher;
import com.overflow.order.repository.OrderRepository;
import com.overflow.order.service.mapper.OrderMapper;
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
  public OrderResponseDto createOrder(OrderRequestDto orderRequestDto){
    log.info("Creating order for productId={} with quantity={}", orderRequestDto.productId(), orderRequestDto.quantity());

    Order order = new Order();
    order.setCustomerName(orderRequestDto.customerName());
    order.setProductId(orderRequestDto.productId());
    order.setQuantity(orderRequestDto.quantity());

    Order savedOrder = orderRepository.save(order);

    orderPublisher.publishOrder(orderMapper.toOrderEventDto(savedOrder));

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
