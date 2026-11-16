package com.backend.orderflow.utils.Rabbitmq.messaging.producer;

import com.backend.orderflow.domain.enuns.OrderStatus;
import com.backend.orderflow.utils.Rabbitmq.RabbitMQConfig;
import com.backend.orderflow.utils.Rabbitmq.messaging.message.OrderStatusMessage;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

import java.util.UUID;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

@ExtendWith(MockitoExtension.class)
class OrderStatusProducerTest {

    @Mock
    private RabbitTemplate rabbitTemplate;

    @InjectMocks
    private OrderStatusProducer orderStatusProducer;

    @Test
    void shouldPublishOrderStatusMessage() {

        // Arrange
        UUID eventId = UUID.randomUUID();
        UUID orderId = UUID.fromString("029bb6fa-c953-4b52-b0e3-be1bb914f89f");
        OrderStatus newStatus = OrderStatus.PROCESSING;

        OrderStatusMessage message = new OrderStatusMessage(
                eventId,
                orderId,
                newStatus
        );

        // Act
        orderStatusProducer.publishStatusChange(message);

        // Assert
        verify(rabbitTemplate).convertAndSend(
                RabbitMQConfig.ORDER_STATUS_EXCHANGE,
                RabbitMQConfig.ORDER_STATUS_ROUTING_KEY,
                message
        );

        verifyNoMoreInteractions(rabbitTemplate);
    }
}
