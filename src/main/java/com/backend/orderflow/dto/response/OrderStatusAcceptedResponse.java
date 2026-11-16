package com.backend.orderflow.dto.response;

import com.backend.orderflow.domain.enuns.OrderStatus;

import java.util.UUID;

public record OrderStatusAcceptedResponse(
        UUID eventId,
        UUID orderId,
        OrderStatus requestedStatus,
        String message
) {
}
