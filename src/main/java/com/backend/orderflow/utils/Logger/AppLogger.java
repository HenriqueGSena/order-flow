package com.backend.orderflow.utils.Logger;

import com.backend.orderflow.domain.enuns.ApplicationLoggerType;
import com.backend.orderflow.dto.request.AppLoggerRequest;
import com.backend.orderflow.service.ApplicationLogService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AppLogger {

    private final ApplicationLogService applicationLogService;

    public void logSuccess(AppLoggerRequest request) {
        applicationLogService.saveLog(
                request,
                ApplicationLoggerType.SUCCESS
        );
    }

    public void logError(AppLoggerRequest request) {
        applicationLogService.saveLog(
                request,
                ApplicationLoggerType.ERROR
        );
    }
}
