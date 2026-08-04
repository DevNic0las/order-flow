package com.overflow.inventory.controller;

import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.overflow.inventory.dto.InventoryProductDto;
import com.overflow.inventory.service.InventoryService;

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
    public ResponseEntity<InventoryProductDto> createProduct(@Valid @RequestBody InventoryProductDto productDto) {
        return ResponseEntity.status(201).body(inventoryService.createProduct(productDto));
    }
    
}
