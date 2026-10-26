package com.backend.orderflow.dto.request;

import jakarta.validation.constraints.*;

import java.math.BigDecimal;

public record ProductRequest(

        @NotBlank(message = "O nome do produto é obrigatório")
        @Size(max = 150, message = "O nome deve ter no máximo 150 caracteres")
        String name,

        @NotBlank(message = "O SKU é obrigatório")
        String sku,

        String description,

        @NotNull(message = "O preço é obrigatório")
        @DecimalMin(value = "0.01", message = "O preço deve ser maior que zero")
        @Digits(integer = 17, fraction = 2, message = "O preço deve ter no máximo 17 dígitos inteiros e 2 decimais")
        BigDecimal price
) {
}
