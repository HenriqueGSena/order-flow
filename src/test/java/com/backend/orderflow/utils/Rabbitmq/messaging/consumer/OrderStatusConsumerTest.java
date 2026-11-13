package com.backend.orderflow.utils.Rabbitmq.messaging.consumer;

import com.backend.orderflow.domain.enuns.OrderStatus;
import com.backend.orderflow.service.OrdersService;
import com.backend.orderflow.utils.Rabbitmq.messaging.message.OrderStatusMessage;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class OrderStatusConsumerTest {

    @Mock
    private OrdersService ordersService;

    @InjectMocks
    private OrderStatusConsumer orderStatusConsumer;

    @Test
    @DisplayName("Should receive OrderStatusMessage and delegate processing to OrdersService")
    void shouldConsumeMessageAndDelegateToOrdersService() {
        // Arrange
        UUID orderId = UUID.randomUUID();
        OrderStatusMessage message = OrderStatusMessage.create(orderId, OrderStatus.PROCESSING);

        // Act
        orderStatusConsumer.consumeOrderStatus(message);

        // Assert
        verify(ordersService).processOrderStatus(message);
    }
}
