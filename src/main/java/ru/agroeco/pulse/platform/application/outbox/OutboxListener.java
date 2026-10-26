package ru.agroeco.pulse.platform.application.outbox;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
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
        try {
            repository.save(OutboxEventEntity.builder()
                .eventType(event.eventType())
                .entityId(event.entityId())
                .payload(mapper.writeValueAsString(event))
                .status(OutboxStatus.NEW)
                .attempts(0)
                .nextAttemptAt(Instant.now())
                .build());
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("Cannot serialize outbox event", ex);
        }
    }
}
