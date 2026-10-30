package ru.agroeco.pulse.platform.application.outbox;

import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import ru.agroeco.pulse.platform.adapter.persistence.outbox.OutboxEventEntity;
import ru.agroeco.pulse.platform.adapter.persistence.outbox.OutboxEventRepository;
import ru.agroeco.pulse.platform.domain.event.DomainEvent;

import java.time.Instant;

@Component
@RequiredArgsConstructor
public class OutboxListener {

    private final OutboxEventRepository repository;
    private final ObjectMapper mapper;

    @TransactionalEventListener(phase = TransactionPhase.BEFORE_COMMIT)
    public void on(DomainEvent event) {
        if (!event.publishToOutbox()) {
            return; // ручной аудит в outbox не пишем
        }

        repository.save(OutboxEventEntity.builder()
                .eventType(event.eventType().getValue())   // AuditAction → String
                .entityId(event.entityId())
                .payload(serialize(event))
                .status(OutboxStatus.NEW)
                .attempts(0)
                .nextAttemptAt(Instant.now())
                .build());
    }

    private String serialize(DomainEvent event) {
        try {
            return mapper.writeValueAsString(event);
        } catch (JacksonException ex) {
            throw new IllegalStateException("Cannot serialize outbox event", ex);
        }
    }
}