package com.orderflow.inventory.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import com.orderflow.inventory.domain.Inventory;
import com.orderflow.inventory.dto.InventoryProductDto;
import com.orderflow.inventory.dto.InventoryResultEventDto;
import com.orderflow.inventory.messaging.InventoryPublisher;
import com.orderflow.inventory.repository.InventoryRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.*;
@ExtendWith(MockitoExtension.class)
class InventoryServiceTest {

@InjectMocks
private InventoryService inventoryService;

@Mock
private InventoryRepository inventoryRepository;

@Mock
private InventoryPublisher inventoryPublisher;

InventoryProductDto inventoryProductDto;

Inventory inventory;

@BeforeEach
void setup(){



    inventory = new Inventory();
    inventory.setId(1L);
    inventory.setQuantity(100);
    inventory.setProductName("Test Product");

      inventoryProductDto = new InventoryProductDto("Test Product", 100);
}

@Test
void shouldDontDecreaseProductStockWhenProuctNotFound(){
    when(inventoryRepository.findById(anyLong())).thenReturn(Optional.empty());

    assertThrows(RuntimeException.class, ()->{
        inventoryService.decreaseProductStock(null, 10L, null, "teste@gmail.com");
    });
    verify(inventoryRepository).findById(10L);
    verify(inventoryRepository, never())
            .save(any(Inventory.class));
}

@Test
void shouldDecreaseProductWhenProductExists(){
when(inventoryRepository.findById(anyLong()))
.thenReturn(Optional.of(inventory));

inventoryService.decreaseProductStock(1L,inventory.getId(),25, "teste@gmail.com");

assertEquals(75, inventory.getQuantity());

verify(inventoryRepository).save(inventory);
verify(inventoryPublisher).publishInventoryResult(any(InventoryResultEventDto.class));


}

@Test
void shouldCreateProductSuccessfully() {

    when(inventoryRepository.save(any(Inventory.class)))
            .thenReturn(inventory);

    InventoryProductDto result = inventoryService.createProduct(inventoryProductDto);

    assertNotNull(result);
    assertEquals("Test Product", result.productName());
    assertEquals(100, result.quantity());

    verify(inventoryRepository).save(any(Inventory.class));
}
}
