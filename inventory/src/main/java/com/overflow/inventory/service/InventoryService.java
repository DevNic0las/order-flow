package com.overflow.inventory.service;

import com.overflow.inventory.domain.Inventory;
import com.overflow.inventory.messaging.InventoryPublisher;
import com.overflow.inventory.repository.InventoryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class InventoryService
{
  private final InventoryRepository inventoryRepository;
  private final Inventory inventory;
  private final InventoryPublisher inventoryPublisher;

  public void decreaseProductStock(Long productId, Integer quantity) {
    log.info("Decreasing stock for productId={} by quantity={}", productId, quantity);

    Inventory inventory = inventoryRepository.findById(productId)
            .orElseThrow(() -> new RuntimeException("Product not found in inventory"));
    inventory.withdraw(quantity);

    inventoryRepository.save(inventory);


    log.info("Stock decreased for productId={} by quantity={}", productId, quantity);

  }

}
