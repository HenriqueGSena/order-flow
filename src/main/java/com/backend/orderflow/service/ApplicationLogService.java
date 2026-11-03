package com.backend.orderflow.service;

import com.backend.orderflow.domain.ApplicationLogger;
import com.backend.orderflow.domain.enuns.ApplicationLoggerType;
import com.backend.orderflow.dto.request.AppLoggerRequest;
import com.backend.orderflow.repository.ApplicationLoggerRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ApplicationLogService {

    private final ApplicationLoggerRepository applicationLogRepository;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void saveLog(
            AppLoggerRequest request,
            ApplicationLoggerType type
    ) {
        var log = new ApplicationLogger();

        log.setOperation(request.operation());
        log.setEntityType(request.entityType());
        log.setEntityId(request.entityId());
        log.setDescription(request.description());
        log.setJsonEnvio(request.jsonEnvio());
        log.setJsonRetorno(request.jsonRetorno());
        log.setErrorMessage(request.errorMessage());
        log.setLogType(type);

        applicationLogRepository.save(log);
    }
}
