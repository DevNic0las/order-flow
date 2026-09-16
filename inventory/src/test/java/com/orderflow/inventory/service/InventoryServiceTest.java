package com.orderflow.inventory.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.orderflow.inventory.domain.Inventory;
import com.orderflow.inventory.dto.InventoryProductDto;
import com.orderflow.inventory.dto.InventoryResultEventDto;
import com.orderflow.inventory.exception.InvalidInventoryQuantityException;
import com.orderflow.inventory.exception.InventoryNotFoundException;
import com.orderflow.inventory.messaging.InventoryPublisher;
import com.orderflow.inventory.repository.InventoryRepository;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class InventoryServiceTest {

   @InjectMocks
   private InventoryService inventoryService;

   @Mock
   private InventoryRepository inventoryRepository;

   @Mock
   private InventoryPublisher inventoryPublisher;

   private InventoryProductDto inventoryProductDto;
   private Inventory inventory;

   @BeforeEach
   void setup() {
       inventory = new Inventory();
       inventory.setId(1L);
       inventory.setQuantity(10);
       inventory.setProductName("Test Product");

       inventoryProductDto = new InventoryProductDto("Test Product", 10);
   }

   @Test
   void shouldDecreaseProductWhenStockIsSufficient() {
       when(inventoryRepository.findById(1L)).thenReturn(Optional.of(inventory));

       inventoryService.decreaseProductStock(99L, 1L, 3, "teste@gmail.com");

       ArgumentCaptor<Inventory> savedInventoryCaptor = ArgumentCaptor.forClass(Inventory.class);
       verify(inventoryRepository).save(savedInventoryCaptor.capture());

       Inventory savedInventory = savedInventoryCaptor.getValue();
       assertEquals(1L, savedInventory.getId());
       assertEquals("Test Product", savedInventory.getProductName());
       assertEquals(7, savedInventory.getQuantity());

       ArgumentCaptor<InventoryResultEventDto> eventCaptor = ArgumentCaptor.forClass(InventoryResultEventDto.class);
       verify(inventoryPublisher).publishInventoryResult(eventCaptor.capture());

       InventoryResultEventDto event = eventCaptor.getValue();
       assertEquals(99L, event.orderId());
       assertTrue(event.approved());
       assertEquals("teste@gmail.com", event.to());
   }

   @Test
   void shouldRejectInsufficientStockWithoutChangingInventory() {
       when(inventoryRepository.findById(1L)).thenReturn(Optional.of(inventory));

       inventoryService.decreaseProductStock(99L, 1L, 15, "teste@gmail.com");

       assertEquals(10, inventory.getQuantity());
       verify(inventoryRepository, never()).save(any(Inventory.class));

       ArgumentCaptor<InventoryResultEventDto> eventCaptor = ArgumentCaptor.forClass(InventoryResultEventDto.class);
       verify(inventoryPublisher).publishInventoryResult(eventCaptor.capture());

       InventoryResultEventDto event = eventCaptor.getValue();
       assertEquals(99L, event.orderId());
       assertFalse(event.approved());
       assertEquals("teste@gmail.com", event.to());
   }

   @Test
   void shouldThrowWhenProductNotFound() {
       when(inventoryRepository.findById(10L)).thenReturn(Optional.empty());

       InventoryNotFoundException exception = assertThrows(
               InventoryNotFoundException.class,
               () -> inventoryService.decreaseProductStock(77L, 10L, 5, "teste@gmail.com")
       );

       assertEquals("Product not found in inventory", exception.getMessage());
       verify(inventoryRepository, never()).save(any(Inventory.class));
       verify(inventoryPublisher, never()).publishInventoryResult(any(InventoryResultEventDto.class));
   }

   @Test
   void shouldRejectInvalidQuantity() {
       InvalidInventoryQuantityException exception = assertThrows(
               InvalidInventoryQuantityException.class,
               () -> inventoryService.decreaseProductStock(77L, 1L, 0, "teste@gmail.com")
       );

       assertEquals("Quantity must be greater than zero", exception.getMessage());
       verifyNoInteractions(inventoryRepository);
       verifyNoInteractions(inventoryPublisher);
   }

   @Test
   void shouldCreateProductSuccessfully() {
       when(inventoryRepository.save(any(Inventory.class))).thenReturn(inventory);

       InventoryProductDto result = inventoryService.createProduct(inventoryProductDto);

       assertNotNull(result);
       assertEquals("Test Product", result.productName());
       assertEquals(10, result.quantity());

       ArgumentCaptor<Inventory> savedInventoryCaptor = ArgumentCaptor.forClass(Inventory.class);
       verify(inventoryRepository).save(savedInventoryCaptor.capture());
       Inventory savedInventory = savedInventoryCaptor.getValue();
       assertEquals("Test Product", savedInventory.getProductName());
       assertEquals(10, savedInventory.getQuantity());
   }
}
