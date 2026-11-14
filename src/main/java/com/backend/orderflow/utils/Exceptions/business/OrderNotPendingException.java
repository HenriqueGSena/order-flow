package com.backend.orderflow.utils.Exceptions.business;


import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

public class OrderNotPendingException extends ResponseStatusException {

    public OrderNotPendingException() {
        super(HttpStatus.CONFLICT, "Não é possível modificar um pedido que não esteja PENDING");
    }
}
