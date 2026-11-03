package com.backend.orderflow.utils.Logger;

import com.backend.orderflow.domain.enuns.ApplicationLoggerType;
import com.backend.orderflow.dto.request.AppLoggerRequest;
import com.backend.orderflow.service.ApplicationLogService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import com.backend.orderflow.domain.enuns.ApplicationLoggerType;
import com.backend.orderflow.dto.request.AppLoggerRequest;
import com.backend.orderflow.service.ApplicationLogService;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class AppLogger {

    private final ApplicationLogService applicationLogService;
    private final ObjectMapper objectMapper;

    public void logSuccess(AppLoggerRequest request) {
        try {
            applicationLogService.saveLog(
                    request,
                    ApplicationLoggerType.SUCCESS
            );
        } catch (Exception exception) {
            log.error("Falha ao registrar log de sucesso: {}", request.operation(), exception);
        }
    }

    public void logError(AppLoggerRequest request) {
        try {
            applicationLogService.saveLog(
                    request,
                    ApplicationLoggerType.ERROR
            );
        } catch (Exception exception) {
            log.error("Falha ao registrar log de erro: {}", request.operation(), exception);
        }
    }

    public String serializeSafely(Object object) {
        if (object == null) {
            return null;
        }

        try {
            return objectMapper.writeValueAsString(object);
        } catch (JacksonException exception) {
            log.error("Falha ao serializar dados para o log", exception);
            return null;
        }
    }
}
