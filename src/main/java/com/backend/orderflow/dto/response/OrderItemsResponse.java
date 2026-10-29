package com.backend.orderflow.dto.response;

import java.math.BigDecimal;
import java.util.UUID;

public record OrderItemsResponse(

        UUID id,
        UUID productId,
        String productName,
        String sku,
        Integer quantity,
        BigDecimal unitPrice,
        BigDecimal subTotal
) {
}
