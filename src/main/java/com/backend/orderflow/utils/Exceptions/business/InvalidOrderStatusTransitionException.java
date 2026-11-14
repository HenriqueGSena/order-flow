package com.backend.orderflow.utils.Exceptions.business;

import com.backend.orderflow.domain.enuns.OrderStatus;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

public class InvalidOrderStatusTransitionException extends ResponseStatusException {

    public InvalidOrderStatusTransitionException(OrderStatus currentStatus, OrderStatus requestedStatus) {
        super(HttpStatus.CONFLICT, "Transição de status não permitida: " + currentStatus + " -> " + requestedStatus);
    }
}
