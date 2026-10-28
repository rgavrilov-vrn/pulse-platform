package ru.agroeco.pulse.outbox.infrastructure;

import java.time.Instant;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import ru.agroeco.pulse.outbox.api.OutboxMessage;
import ru.agroeco.pulse.outbox.api.OutboxPort;
import ru.agroeco.pulse.outbox.domain.OutboxEvent;
import ru.agroeco.pulse.outbox.infrastructure.persistence.OutboxEventRepository;

@Component
public class OutboxJpaAdapter implements OutboxPort {

    private final OutboxEventRepository repository;

    public OutboxJpaAdapter(OutboxEventRepository repository) {
        this.repository = repository;
    }

    @Override
    @Transactional
    public void publish(OutboxMessage message) {
        OutboxEvent event = new OutboxEvent(
            message.eventId(),
            message.eventType(),
            message.entityId(),
            message.payload(),
            Instant.now()
        );
        repository.save(event);
    }
}
