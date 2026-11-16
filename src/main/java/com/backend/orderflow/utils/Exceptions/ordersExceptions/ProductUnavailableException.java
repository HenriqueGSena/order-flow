package com.backend.orderflow.utils.Exceptions.ordersExceptions;


import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

public class ProductUnavailableException extends ResponseStatusException {

    public ProductUnavailableException(String productName) {
        super(HttpStatus.UNPROCESSABLE_ENTITY, "Produto indisponível: " + productName);
    }
}
