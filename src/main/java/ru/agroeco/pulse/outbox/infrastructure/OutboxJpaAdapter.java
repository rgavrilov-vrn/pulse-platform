package ru.agroeco.pulse.outbox.infrastructure;

import java.time.Instant;
import java.util.UUID;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import ru.agroeco.pulse.outbox.api.OutboxMessage;
import ru.agroeco.pulse.outbox.api.OutboxPort;
import ru.agroeco.pulse.platform.adapter.persistence.outbox.OutboxEventEntity;
import ru.agroeco.pulse.platform.adapter.persistence.outbox.OutboxEventRepository;
import ru.agroeco.pulse.platform.application.outbox.OutboxStatus;

@Component
public class OutboxJpaAdapter implements OutboxPort {

    private final OutboxEventRepository repository;

    public OutboxJpaAdapter(OutboxEventRepository repository) {
        this.repository = repository;
    }

    @Override
    @Transactional
    public void publish(OutboxMessage message) {
        OutboxEventEntity event = OutboxEventEntity.builder()
            .id(message.eventId())
            .eventType(message.eventType())
            .entityId(message.entityId())
            .payload(message.payload())
            .status(OutboxStatus.NEW)
            .attempts(0)
            .nextAttemptAt(Instant.now())
            .createdAt(Instant.now())
            .build();

        repository.save(event);
    }
}
