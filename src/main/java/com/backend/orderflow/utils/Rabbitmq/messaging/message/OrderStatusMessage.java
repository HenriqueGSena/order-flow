package com.backend.orderflow.utils.Rabbitmq.messaging.message;

import com.backend.orderflow.domain.enuns.OrderStatus;

import java.util.UUID;

public record OrderStatusMessage(
        UUID eventId,
        UUID orderId,
        OrderStatus newStatus
) {

    public static OrderStatusMessage create(UUID orderId, OrderStatus newStatus) {
        return new OrderStatusMessage(
                UUID.randomUUID(),
                orderId,
                newStatus
        );
    }
}
