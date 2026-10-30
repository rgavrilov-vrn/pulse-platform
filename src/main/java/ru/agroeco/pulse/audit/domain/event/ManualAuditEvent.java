package ru.agroeco.pulse.audit.domain.event;

import lombok.Builder;
import ru.agroeco.pulse.platform.domain.event.DomainEventType;
import ru.agroeco.pulse.platform.domain.event.DomainEvent;

import java.time.Instant;
import java.util.UUID;

@Builder
public record ManualAuditEvent(
        DomainEventType action,
        String schema,
        String table,
        UUID entityId,
        UUID actorId,
        Object oldData,
        Object newData,
        Instant occurredAt
) implements DomainEvent {

    @Override
    public DomainEventType eventType() {
        return action;
    }

    @Override
    public String entitySchema() {
        return schema;
    }

    @Override
    public String entityTable() {
        return table;
    }

    // entityId() — генерируется автоматически из компонента
    // occurredAt() — генерируется автоматически (null допустим → листенер подставит now)

    @Override
    public Object oldDataSnapshot() {
        return oldData;
    }

    @Override
    public Object newDataSnapshot() {
        return newData;
    }

    @Override
    public UUID actorId() {
        return actorId;
    }

    @Override
    public boolean publishToOutbox() {
        return false;  // аудит не уходит в outbox
    }
}