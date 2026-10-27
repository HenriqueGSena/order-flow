package com.backend.orderflow.dto.request;

import com.backend.orderflow.domain.enuns.ProductStatus;
import jakarta.validation.constraints.NotNull;

public record ProductStatusRequest(
        @NotNull(message = "O status do produto é obrigatório")
        ProductStatus status
) {}
