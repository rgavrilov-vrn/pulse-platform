package ru.agroeco.pulse.platform.application.outbox;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import tools.jackson.databind.ObjectMapper;
import ru.agroeco.pulse.platform.adapter.persistence.outbox.OutboxEventEntity;
import ru.agroeco.pulse.platform.adapter.persistence.outbox.OutboxEventRepository;
import ru.agroeco.pulse.platform.domain.event.DomainEvent;

import java.time.Instant;

@Slf4j
@Component
@RequiredArgsConstructor
public class OutboxListener {

    private final OutboxEventRepository repository;
    private final ObjectMapper mapper;

    @TransactionalEventListener(phase = TransactionPhase.BEFORE_COMMIT)
    public void on(DomainEvent event) {
        if (!event.publishToOutbox()) {
            return;
        }
        repository.save(OutboxEventEntity.builder()
                .eventType(event.outboxEventType())
                .entityId(event.entityId())
                .payload(serialize(event.outboxPayload()))
                .status(OutboxStatus.NEW)
                .attempts(0)
                .nextAttemptAt(Instant.now())
                .build());
    }

    private String serialize(Object payload) {
        if (payload == null) return "{}";
        try {
            return mapper.writeValueAsString(payload);
        } catch (RuntimeException ex) {
            throw new IllegalStateException("Cannot serialize outbox payload", ex);
        }
    }
}