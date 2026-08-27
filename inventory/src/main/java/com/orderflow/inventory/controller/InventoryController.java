package com.orderflow.inventory.controller;

import com.orderflow.inventory.dto.InventoryProductDto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;


import com.orderflow.inventory.service.InventoryService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;


@RestController
@RequestMapping("/inventory")
@RequiredArgsConstructor
public class InventoryController {
    private final InventoryService inventoryService;

    @PostMapping("/products")
    @PreAuthorize("hasRole('ADMIN')")

    public ResponseEntity<InventoryProductDto> createProduct(@Valid @RequestBody InventoryProductDto productDto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(inventoryService.createProduct(productDto));
    }
    
}
