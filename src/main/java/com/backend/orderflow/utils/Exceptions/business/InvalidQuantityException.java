package com.backend.orderflow.utils.Exceptions.business;


import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

public class InvalidQuantityException extends ResponseStatusException {

    public InvalidQuantityException() {
        super(HttpStatus.BAD_REQUEST, "A quantidade deve ser maior que zero");
    }
}
