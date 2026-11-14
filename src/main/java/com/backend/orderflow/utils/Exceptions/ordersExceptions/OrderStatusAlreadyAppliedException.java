package com.backend.orderflow.utils.Exceptions.ordersExceptions;

import com.backend.orderflow.domain.enuns.OrderStatus;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

public class OrderStatusAlreadyAppliedException extends ResponseStatusException {

    public OrderStatusAlreadyAppliedException(OrderStatus status) {
        super(HttpStatus.CONFLICT, "O pedido já possui esse status: " + status);
    }
}
