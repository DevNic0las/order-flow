package com.orderflow.inventory.service;

import com.orderflow.inventory.domain.Inventory;
import com.orderflow.inventory.dto.InventoryProductDto;
import com.orderflow.inventory.dto.InventoryResultEventDto;
import com.orderflow.inventory.exception.InventoryNotFoundException;
import com.orderflow.inventory.messaging.InventoryPublisher;
import com.orderflow.inventory.repository.InventoryRepository;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class InventoryService
{
  private final InventoryRepository inventoryRepository;
  private final InventoryPublisher inventoryPublisher;

  public void decreaseProductStock(Long orderId, Long productId, Integer quantity, String email) {
    log.info("Decreasing stock for productId={} by quantity={}", productId, quantity);

    Inventory inventory = inventoryRepository.findById(productId)
            .orElseThrow(() -> new InventoryNotFoundException("Product not found in inventory"));

    boolean approved = inventory.withdraw(quantity);

    if (approved) {
        inventoryRepository.save(inventory);
        log.info("Stock decreased for productId={} by quantity={}", productId, quantity);
    } else {
        log.warn("Insufficient stock for productId={}, requested={}, available={}",
                productId, quantity, inventory.getQuantity());
    }

    InventoryResultEventDto result = new InventoryResultEventDto(orderId, approved, email);
    inventoryPublisher.publishInventoryResult(result);
}

@Transactional
public InventoryProductDto createProduct(InventoryProductDto productDto) {
    Inventory inventory = new Inventory();
    inventory.setProductName(productDto.productName());
    inventory.setQuantity(productDto.quantity());
    inventoryRepository.save(inventory);
    log.info("Created new product in inventory: {}", productDto);
    return new InventoryProductDto(inventory.getProductName(), inventory.getQuantity());
}

public List<InventoryProductDto> getAllProducts() {
    List<Inventory> inventory = inventoryRepository.findAll();
    return inventory.stream().map(i-> new InventoryProductDto(i.getProductName(),i.getQuantity())).toList();
}
}
