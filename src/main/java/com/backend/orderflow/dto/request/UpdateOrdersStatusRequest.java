package com.backend.orderflow.dto.request;

import com.backend.orderflow.domain.enuns.OrderStatus;
import jakarta.validation.constraints.NotNull;

public record UpdateOrdersStatusRequest(

        @NotNull(message = "O status do pedido é obrigatório")
        OrderStatus orderStatus
) {
}
