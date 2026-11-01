package com.backend.orderflow.mapper;

import com.backend.orderflow.domain.enuns.OrderStatus;
import com.backend.orderflow.dto.OrderDetailsInterface;
import com.backend.orderflow.dto.response.OrderItemsResponse;
import com.backend.orderflow.dto.response.OrdersResponse;

import java.math.BigDecimal;
import java.util.*;

public final class OrdersDetailsMapper {

    private OrdersDetailsMapper() {}

    public static List<OrdersResponse> toResponse(
            List<OrderDetailsInterface> rows
    ) {

        Map<UUID, List<OrderDetailsInterface>> groupedOrders =
                new LinkedHashMap<>();

        for (var row : rows) {
            groupedOrders
                    .computeIfAbsent(row.getOrderId(), key -> new ArrayList<>())
                    .add(row);
        }

        return groupedOrders.values()
                .stream()
                .map(OrdersDetailsMapper::toOrderResponse)
                .toList();
    }

    private static OrdersResponse toOrderResponse(
            List<OrderDetailsInterface> rows
    ) {

        var order = rows.get(0);

        var items = rows.stream()
                .filter(row -> row.getOrderItemId() != null)
                .map(OrdersDetailsMapper::toItemResponse)
                .toList();

        var totalAmount = items.stream()
                .map(OrderItemsResponse::subTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return new OrdersResponse(
                order.getOrderId(),
                order.getOrderDate(),
                OrderStatus.valueOf(order.getOrderStatus()),
                items,
                totalAmount
        );
    }

    private static OrderItemsResponse toItemResponse(
            OrderDetailsInterface row
    ) {

        var subtotal = row.getUnitPrice()
                .multiply(BigDecimal.valueOf(row.getQuantity()));

        return new OrderItemsResponse(
                row.getOrderItemId(),
                row.getProductId(),
                row.getProductName(),
                row.getSku(),
                row.getQuantity(),
                row.getUnitPrice(),
                subtotal
        );
    }
}
