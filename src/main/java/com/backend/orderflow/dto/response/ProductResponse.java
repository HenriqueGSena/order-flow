package com.backend.orderflow.dto.response;

import com.backend.orderflow.domain.enuns.ProductStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public record ProductResponse(

        UUID id,
        String name,
        String sku,
        ProductStatus status,
        String description,
        BigDecimal price,
        LocalDateTime createdAt,
        LocalDateTime updatedAt

) {
}
