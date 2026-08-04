package com.overflow.order.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.overflow.order.domain.Order;
import com.overflow.order.domain.OrderStatus;
import com.overflow.order.dtos.OrderEventDto;
import com.overflow.order.dtos.OrderRequestDto;
import com.overflow.order.dtos.OrderResponseDto;
import com.overflow.order.exception.OrderNotFoundException;
import com.overflow.order.messaging.OrderPublisher;
import com.overflow.order.repository.OrderRepository;
import com.overflow.order.service.mapper.OrderMapper;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    @InjectMocks
    private OrderService orderService;

    @Mock
    private OrderPublisher orderPublisher;

    @Mock
    private OrderMapper orderMapper;

    @Mock
    private OrderRepository orderRepository;

    private OrderRequestDto orderRequestDto;
    private Order order;

    @BeforeEach
    void setup() {
        orderRequestDto = new OrderRequestDto("Customer Test", 123L, 5);

        order = new Order();
        order.setId(1L);
        order.setCustomerName(orderRequestDto.customerName());
        order.setProductId(orderRequestDto.productId());
        order.setQuantity(orderRequestDto.quantity());
        order.setStatus(OrderStatus.PENDING);
    }

    @Test
    void shouldCreateOrderAndPublishEvent() {
        when(orderRepository.save(any(Order.class))).thenReturn(order);
        when(orderMapper.toOrderEventDto(order)).thenReturn(new OrderEventDto(order.getId(), order.getProductId(), order.getQuantity()));
        when(orderMapper.toResponseDto(order)).thenReturn(new OrderResponseDto(order.getId(), order.getCustomerName(), order.getProductId(), order.getQuantity(), order.getStatus().name(), null));

        OrderResponseDto result = orderService.createOrder(orderRequestDto);

        assertNotNull(result);
        assertEquals(order.getId(), result.id());
        assertEquals(order.getCustomerName(), result.customerName());
        verify(orderRepository).save(any(Order.class));
        verify(orderPublisher).publishOrder(any(OrderEventDto.class));
    }

    @Test
    void shouldConfirmExistingOrder() {
        when(orderRepository.findById(anyLong())).thenReturn(Optional.of(order));

        orderService.confirmOrder(1L);

        assertEquals(OrderStatus.CONFIRMED, order.getStatus());
        verify(orderRepository).save(order);
    }

    @Test
    void shouldRejectExistingOrder() {
        when(orderRepository.findById(anyLong())).thenReturn(Optional.of(order));

        orderService.rejectOrder(1L);

        assertEquals(OrderStatus.REJECTED, order.getStatus());
        verify(orderRepository).save(order);
    }

    @Test
    void shouldThrowWhenConfirmingMissingOrder() {
        when(orderRepository.findById(anyLong())).thenReturn(Optional.empty());

        assertThrows(OrderNotFoundException.class, () -> orderService.confirmOrder(1L));
    }

    @Test
    void shouldThrowWhenRejectingMissingOrder() {
        when(orderRepository.findById(anyLong())).thenReturn(Optional.empty());

        assertThrows(OrderNotFoundException.class, () -> orderService.rejectOrder(1L));
    }
}
