package com.backend.orderflow.utils.ApiResponse;

public record Response<T>(
        T data,
        Integer statusCode,
        String message
) {
}
