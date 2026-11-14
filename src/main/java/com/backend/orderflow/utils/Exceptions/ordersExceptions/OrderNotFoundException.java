package com.backend.orderflow.utils.Exceptions.ordersExceptions;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.UUID;

public class OrderNotFoundException extends ResponseStatusException {

    public OrderNotFoundException(UUID orderId) {
        super(HttpStatus.NOT_FOUND, "Pedido não encontrado com ID informado: " + orderId);
    }
}
