package com.overflow.order.controller;

import com.overflow.order.dtos.OrderRequestDto;
import com.overflow.order.dtos.OrderResponseDto;
import com.overflow.order.service.OrderService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RequestMapping("/orders")
@RestController
@RequiredArgsConstructor
public class OrderController {

private final OrderService orderService;


@PostMapping
  public ResponseEntity<OrderResponseDto> createOrder( @Valid @RequestBody OrderRequestDto orderRequestDto){
  return ResponseEntity.ok(orderService.createOrder(orderRequestDto));
}

}
