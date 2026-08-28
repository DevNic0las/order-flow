package com.orderflow.order.controller;

import com.orderflow.order.dtos.OrderRequestDto;
import com.orderflow.order.dtos.OrderResponseDto;
import com.orderflow.order.service.OrderService;

import io.jsonwebtoken.Jwt;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RequestMapping("/orders")
@RestController
@RequiredArgsConstructor
public class OrderController {

private final OrderService orderService;


@PostMapping

  public ResponseEntity<OrderResponseDto> createOrder(   Authentication authentication,
                                                         @Valid @RequestBody OrderRequestDto orderRequestDto){
  String userId = authentication.getName();
  return ResponseEntity.ok(orderService.createOrder(orderRequestDto, userId));
}

@GetMapping("/test")
  public String test(){
  return "test";
}

}
