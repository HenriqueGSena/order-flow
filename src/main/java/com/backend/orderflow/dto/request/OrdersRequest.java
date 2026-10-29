package com.backend.orderflow.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;

public record OrdersRequest(

        @NotEmpty(message = "O pedido deve possuir pelo menos um item")
        List<@Valid OrderItemsRequest> items
) {
}
