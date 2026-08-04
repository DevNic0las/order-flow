package com.overflow.inventory.service;

import com.overflow.inventory.domain.Inventory;
import com.overflow.inventory.dto.InventoryEventDto;
import com.overflow.inventory.dto.InventoryProductDto;
import com.overflow.inventory.dto.InventoryResultEventDto;
import com.overflow.inventory.messaging.InventoryPublisher;
import com.overflow.inventory.repository.InventoryRepository;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class InventoryService
{
  private final InventoryRepository inventoryRepository;
  private final InventoryPublisher inventoryPublisher;

  public void decreaseProductStock(Long orderId, Long productId, Integer quantity) {
    log.info("Decreasing stock for productId={} by quantity={}", productId, quantity);

    Inventory inventory = inventoryRepository.findById(productId)
            .orElseThrow(() -> new RuntimeException("Product not found in inventory"));

    boolean approved = inventory.withdraw(quantity);

    if (approved) {
        inventoryRepository.save(inventory);
        log.info("Stock decreased for productId={} by quantity={}", productId, quantity);
    } else {
        log.warn("Insufficient stock for productId={}, requested={}, available={}",
                productId, quantity, inventory.getQuantity());
    }

    InventoryResultEventDto result = new InventoryResultEventDto(orderId, approved);
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
}
