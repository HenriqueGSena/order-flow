package com.backend.orderflow.utils.Exceptions.ordersExceptions;


import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

public class OrderWithoutItemsException extends ResponseStatusException {

    public OrderWithoutItemsException() {
        super(HttpStatus.UNPROCESSABLE_ENTITY, "O pedido precisa possuir pelo menos um item");
    }
}
