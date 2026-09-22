package com.orderflow.inventory.messaging;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.orderflow.inventory.exception.InventoryGlobalExceptionHandler;
import com.orderflow.inventory.service.InventoryService;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.AmqpRejectAndDontRequeueException;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.support.converter.MessageConversionException;
import org.springframework.amqp.support.converter.MessageConverter;

class InventoryConsumerTest {

    @Test
    void shouldRejectMalformedMessageWithoutRequeue() {
        InventoryService inventoryService = mock(InventoryService.class);
        MessageConverter messageConverter = mock(MessageConverter.class);
        InventoryConsumer consumer = new InventoryConsumer(inventoryService, messageConverter);

        MessageProperties props = new MessageProperties();
        props.setMessageId("msg-123");
        Message message = new Message("{bad-json".getBytes(StandardCharsets.UTF_8), props);

        when(messageConverter.fromMessage(message)).thenThrow(new MessageConversionException("bad json"));

        AmqpRejectAndDontRequeueException ex = assertThrows(
                AmqpRejectAndDontRequeueException.class,
                () -> consumer.onOrderInventoryResult(message)
        );

        assertEquals("Malformed inventory message, sending to DLQ", ex.getMessage());
        verifyNoInteractions(inventoryService);
    }

    @Test
    void shouldReturnGeneric500ForUnhandledException() {
        InventoryGlobalExceptionHandler handler = new InventoryGlobalExceptionHandler();

        var response = handler.handleUnexpectedException(new RuntimeException("internal detail"));

        assertEquals(500, response.getStatusCode().value());
        assertEquals("An unexpected error occurred", response.getBody().message());
    }
}
