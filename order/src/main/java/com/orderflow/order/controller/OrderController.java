package com.orderflow.order.controller;

import com.orderflow.order.dtos.OrderRequestDto;
import com.orderflow.order.dtos.OrderResponseDto;
import com.orderflow.order.service.OrderService;
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
