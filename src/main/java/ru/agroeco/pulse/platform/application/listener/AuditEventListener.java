package ru.agroeco.pulse.platform.application.listener;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import ru.agroeco.pulse.platform.adapter.persistence.audit.AuditEventEntity;
import ru.agroeco.pulse.platform.adapter.persistence.audit.AuditEventRepository;
import ru.agroeco.pulse.platform.domain.event.DomainEvent;

@Component
@RequiredArgsConstructor
public class AuditEventListener {
    private final AuditEventRepository repository;
    private final ObjectMapper mapper;

    @TransactionalEventListener(phase = TransactionPhase.BEFORE_COMMIT)
    public void on(DomainEvent event) {
        try {
            repository.save(AuditEventEntity.builder()
                .action(event.eventType())
                .actorId(event.entityId())
                .entitySchema(event.entitySchema())
                .entityTable(event.entityTable())
                .entityId(event.entityId())
                .newData(mapper.writeValueAsString(event))
                .occurredAt(event.occurredAt())
                .build());
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("Cannot serialize audit event", ex);
        }
    }
}
