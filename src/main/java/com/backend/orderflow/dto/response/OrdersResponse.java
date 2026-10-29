package com.backend.orderflow.dto.response;

import com.backend.orderflow.domain.enuns.OrderStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public record OrdersResponse(

        UUID id,
        LocalDateTime orderDate,
        OrderStatus orderStatus,
        List<OrderItemsResponse> items,
        BigDecimal totalAmount,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
