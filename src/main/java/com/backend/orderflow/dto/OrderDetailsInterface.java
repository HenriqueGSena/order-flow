package com.backend.orderflow.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public interface OrderDetailsInterface {

    UUID getOrderId();

    LocalDateTime getOrderDate();

    String getOrderStatus();

    UUID getOrderItemId();

    UUID getProductId();

    String getProductName();

    String getSku();

    Integer getQuantity();

    BigDecimal getUnitPrice();
}
