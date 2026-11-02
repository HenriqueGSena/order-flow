package com.backend.orderflow.utils.Logger;

import com.backend.orderflow.dto.request.AppLoggerSuccessEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
public class AppLoggerListener {

    private final AppLogger applicationLogger;

    @TransactionalEventListener(
            phase = TransactionPhase.AFTER_COMMIT
    )
    public void handleSuccess(AppLoggerSuccessEvent event) {
        applicationLogger.logSuccess(event.request());
    }
}
