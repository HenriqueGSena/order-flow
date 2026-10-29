package com.backend.orderflow.mapper;

import com.backend.orderflow.domain.Order;
import com.backend.orderflow.domain.OrderItems;
import com.backend.orderflow.dto.response.OrderItemsResponse;
import com.backend.orderflow.dto.response.OrdersResponse;

import java.math.BigDecimal;
import java.util.List;

public final class OrdersMapper {

    private OrdersMapper() {
    }

    public static OrdersResponse toResponse(Order order) {
        List<OrderItemsResponse> items = order.getItems()
                .stream()
                .map(OrdersMapper::toItemResponse)
                .toList();

        BigDecimal totalAmount = items.stream()
                .map(OrderItemsResponse::subTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return new OrdersResponse(
                order.getId(),
                order.getOrderDate(),
                order.getOrderStatus(),
                items,
                totalAmount,
                order.getCreatedAt(),
                order.getUpdatedAt()
        );
    }

    private static OrderItemsResponse toItemResponse(OrderItems item) {
        return new OrderItemsResponse(
                item.getId(),
                item.getProduct().getId(),
                item.getProduct().getName(),
                item.getProduct().getSku(),
                item.getQuantity(),
                item.getUnitPrice(),
                item.getSubtotal()
        );
    }
}
