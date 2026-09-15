package com.orderflow.inventory.controller;

import com.orderflow.inventory.dto.InventoryProductDto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import org.springframework.data.jpa.repository.Query;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;


import com.orderflow.inventory.service.InventoryService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

import org.springframework.http.ResponseEntity;

import java.util.List;


@RestController
@RequiredArgsConstructor
public class InventoryController {
    private final InventoryService inventoryService;


    @PostMapping("/products")
    @PreAuthorize("hasRole('ADMIN')")

    public ResponseEntity<InventoryProductDto> createProduct(@Valid @RequestBody InventoryProductDto productDto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(inventoryService.createProduct(productDto));
    }

    @GetMapping("/products")
    public ResponseEntity<List<InventoryProductDto>> getProducts() {
        return ResponseEntity.status(HttpStatus.OK).body(inventoryService.getAllProducts());
    }

}
