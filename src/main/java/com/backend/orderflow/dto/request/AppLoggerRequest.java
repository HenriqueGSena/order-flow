package com.backend.orderflow.dto.request;

public record AppLoggerRequest(
        String operation,
        String entityType,
        String entityId,
        String description,
        String jsonEnvio,
        String jsonRetorno,
        String errorMessage
) {
}
