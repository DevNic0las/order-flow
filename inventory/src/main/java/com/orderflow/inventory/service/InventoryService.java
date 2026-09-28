package com.orderflow.inventory.service;

import com.orderflow.inventory.domain.Inventory;
import com.orderflow.inventory.domain.ProcessedInventoryEvent;
import com.orderflow.inventory.dto.InventoryProductDto;
import com.orderflow.inventory.dto.InventoryResultEventDto;
import com.orderflow.inventory.exception.InvalidInventoryQuantityException;
import com.orderflow.inventory.exception.InventoryNotFoundException;
import com.orderflow.inventory.messaging.InventoryPublisher;
import com.orderflow.inventory.repository.InventoryIdempotencyKeyRepository;
import com.orderflow.inventory.repository.InventoryRepository;

import com.orderflow.inventory.repository.ProcessedInventoryEventRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.flywaydb.core.internal.util.StringUtils;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class InventoryService
{
  private final InventoryRepository inventoryRepository;
  private final InventoryPublisher inventoryPublisher;
  private final ProcessedInventoryEventRepository processedInventoryEventRepository;
  private final InventoryIdempotencyKeyRepository inventoryIdempotencyKeyRepo;
  @Transactional
  public void decreaseProductStock(
          UUID eventId,
          Long orderId,
          Long productId,
          Integer quantity,
          String email) {

    if (processedInventoryEventRepository.existsByEventId(eventId)) {
      log.warn("Event already processed. Skipping processing. eventId={}", eventId);
      return;
    }

    if (quantity == null || quantity <= 0) {
      throw new InvalidInventoryQuantityException(
              "Quantity must be greater than zero"
      );
    }

    // Try to create a processed-event record idempotently using a DB upsert (INSERT ... ON CONFLICT DO NOTHING)
    // insertIfNotExists returns number of rows inserted (1) or 0 if already present
    int inserted = processedInventoryEventRepository.insertIfNotExists(eventId);
    if (inserted == 0) {
      log.info("Event already processed, ignoring. eventId={}", eventId);
      return;
    }

    log.info(
            "Decreasing stock for productId={} by quantity={}",
            productId,
            quantity
    );

    Inventory inventory = inventoryRepository.findById(productId)
            .orElseThrow(() ->
                    new InventoryNotFoundException(
                            "Product not found in inventory"
                    )
            );

    boolean approved = inventory.withdraw(quantity);

    if (approved) {
      inventoryRepository.save(inventory);

      log.info(
              "Stock decreased for productId={} by quantity={}",
              productId,
              quantity
      );
    } else {
      log.warn(
              "Insufficient stock for productId={}, requested={}, available={}",
              productId,
              quantity,
              inventory.getQuantity()
      );
    }

    InventoryResultEventDto result =
            new InventoryResultEventDto(orderId, approved, email, eventId);

    inventoryPublisher.publishInventoryResult(result);
  }

@Transactional
public InventoryProductDto createProduct(InventoryProductDto productDto, String idempotencyKey) {

  if(StringUtils.hasText(idempotencyKey)){
      Optional<Long> existingInventoryId = inventoryIdempotencyKeyRepo.findInventoryIdByIdempotencyKey(idempotencyKey);
      if(existingInventoryId.isPresent()){
          Long inventoryId = existingInventoryId.get();
          Inventory inventory = inventoryRepository.findById(inventoryId)
                  .orElseThrow(() -> new IllegalStateException("Inventory not found for idempotency key: " + idempotencyKey));
          log.info("Returning existing inventory for idempotency key. inventoryId={}, key={}", inventoryId, idempotencyKey);
          return new InventoryProductDto(inventory.getId(), inventory.getProductName(), inventory.getQuantity());
      }
  }

    Inventory inventory = new Inventory();
    inventory.setProductName(productDto.productName());
    inventory.setQuantity(productDto.quantity());
    inventoryRepository.save(inventory);
    log.info("Created new product in inventory: {}", productDto);
    return new InventoryProductDto(inventory.getId(), inventory.getProductName(), inventory.getQuantity());
}

public List<InventoryProductDto> getAllProducts() {
    List<Inventory> inventory = inventoryRepository.findAll();
    return inventory.stream().map(i-> new InventoryProductDto(i.getId(), i.getProductName(),i.getQuantity())).toList();
}
}
