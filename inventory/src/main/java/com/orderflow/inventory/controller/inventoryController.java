package com.orderflow.inventory.controller;

import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.orderflow.inventory.dto.InventoryProductDto;
import com.orderflow.inventory.service.InventoryService;

import lombok.RequiredArgsConstructor;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;


@RestController
@RequestMapping("/inventory")
@RequiredArgsConstructor
public class inventoryController {
    private final InventoryService inventoryService;

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<InventoryProductDto> createProduct(@RequestBody InventoryProductDto productDto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(inventoryService.createProduct(productDto));
    }
    
}
