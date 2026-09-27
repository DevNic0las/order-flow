package com.orderflow.inventory.messaging;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

import com.orderflow.inventory.dto.InventoryEventDto;
import com.orderflow.inventory.exception.InventoryGlobalExceptionHandler;
import com.orderflow.inventory.service.InventoryService;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.amqp.AmqpRejectAndDontRequeueException;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.support.converter.MessageConversionException;
import org.springframework.amqp.support.converter.MessageConverter;

class InventoryConsumerTest {


    @Test
    void shouldDecreaseStockWhenMessageIsValid() {
        InventoryService inventoryService = mock(InventoryService.class);

        InventoryConsumer consumer = new InventoryConsumer(inventoryService);

        InventoryEventDto event = new InventoryEventDto(
                1L,
                10L,
                2,
                "EMAIL",
                UUID.randomUUID()
                );

        consumer.onOrderInventoryResult(event);

        verify(inventoryService).decreaseProductStock(
                event.eventId(),
                event.orderId(),
                event.productId(),
                event.quantity(),
                event.to()
        );
    }

    @Test
    void shouldReturnGeneric500ForUnhandledException() {
        InventoryGlobalExceptionHandler handler = new InventoryGlobalExceptionHandler();

        var response = handler.handleUnexpectedException(new RuntimeException("internal detail"));

        assertEquals(500, response.getStatusCode().value());
        assertEquals("An unexpected error occurred", response.getBody().message());
    }
}
