package com.orderflow.inventory.integration;


import com.orderflow.inventory.domain.Inventory;
import com.orderflow.inventory.repository.InventoryRepository;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

@Service
public class InventoryConcurrencyTestService {

  private final InventoryRepository inventoryRepository;

  public InventoryConcurrencyTestService(InventoryRepository inventoryRepository) {
    this.inventoryRepository = inventoryRepository;
  }

  @Transactional
  public Inventory findInventory(Long id) {
    return inventoryRepository.findById(id)
            .orElseThrow();
  }


}
