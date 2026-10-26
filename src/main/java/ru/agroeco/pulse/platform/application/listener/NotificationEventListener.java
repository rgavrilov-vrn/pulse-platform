package ru.agroeco.pulse.platform.application.listener;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import ru.agroeco.pulse.platform.domain.event.*;

@Component
@Slf4j
public class NotificationEventListener {
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void on(UserAuthenticated e) {
        log.info("Notification: new UserAuthenticated {}", e.username());
    }
}
