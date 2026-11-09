package com.backend.orderflow.utils.Rabbitmq.messaging.producer;

import com.backend.orderflow.utils.Rabbitmq.RabbitMQConfig;
import com.backend.orderflow.utils.Rabbitmq.messaging.message.OrderStatusMessage;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class OrderStatusProducer {

    private final RabbitTemplate rabbitTemplate;

    public void publishStatusChange(OrderStatusMessage message) {

        rabbitTemplate.convertAndSend(
                RabbitMQConfig.ORDER_STATUS_EXCHANGE,
                RabbitMQConfig.ORDER_STATUS_ROUTING_KEY,
                message
        );
    }
}
