package com.backend.orderflow.utils.Exceptions.business;


import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

public class OrderItemNotFoundException extends ResponseStatusException {

    public OrderItemNotFoundException() {
        super(HttpStatus.NOT_FOUND, "Item não encontrado neste pedido");
    }
}
