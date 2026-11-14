package com.backend.orderflow.utils.Exceptions.business;

import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

public class ProductNotFoundException extends ResponseStatusException {

    public ProductNotFoundException(UUID productId) {
        super(HttpStatus.NOT_FOUND, "Produto não encontrado: " + productId);
    }
}
