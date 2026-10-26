
package com.backend.orderflow.utils.ApiResponse;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

public final class ApiResponses {

    private ApiResponses() {
    }

    public static <T> ResponseEntity<Response<T>> success(
            T data,
            String message,
            HttpStatus status
    ) {
        var response = new Response<>(
                data,
                status.value(),
                message
        );

        return ResponseEntity.status(status).body(response);
    }

    public static <T> ResponseEntity<Response<T>> error(
            String message,
            HttpStatus status
    ) {
        var response = new Response<T>(
                null,
                status.value(),
                message
        );

        return ResponseEntity.status(status).body(response);
    }
}
