package com.backend.orderflow.repository;

import com.backend.orderflow.domain.Order;
import com.backend.orderflow.dto.OrderDetailsInterface;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface OrdersRepository extends JpaRepository<Order, UUID> {

    Optional<Order> findById(UUID id);

    @Query(value = """
    SELECT
        o.id AS orderId,
        o.order_date AS orderDate,
        o.order_status AS orderStatus,
        oi.id AS orderItemId,
        oi.product_id AS productId,
        p.name AS productName,
        p.sku AS sku,
        oi.quantity AS quantity,
        oi.unit_price AS unitPrice
    FROM orders o
    LEFT JOIN order_items oi ON o.id = oi.order_id
    LEFT JOIN product p ON oi.product_id = p.id
    ORDER BY o.order_date DESC
    """, nativeQuery = true)
    List<OrderDetailsInterface> findAllOrderDetails();
}
