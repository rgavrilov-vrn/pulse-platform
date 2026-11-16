package ru.agroeco.pulse.incident.adapter.messaging;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import ru.agroeco.pulse.incident.application.CreateIncidentFromManualEventUseCase;
import ru.agroeco.pulse.outbox.api.ProcessedEventPort;
import ru.agroeco.pulse.trassir.api.event.TrassirManualIncidentRequested;

@Component
@RequiredArgsConstructor
public class TrassirManualIncidentIncidentListener {

    private final CreateIncidentFromManualEventUseCase useCase;
    private final ProcessedEventPort processedEventPort;

    @TransactionalEventListener(phase = TransactionPhase.BEFORE_COMMIT)
    public void on(TrassirManualIncidentRequested event) {
        if (!processedEventPort.tryMarkProcessed("incident-manual", event.eventId())) {
            return;
        }
        useCase.handle(event);
    }
}