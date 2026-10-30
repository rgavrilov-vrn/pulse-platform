package ru.agroeco.pulse.refs.domain.event;

import lombok.Builder;
import ru.agroeco.pulse.platform.domain.event.DomainEventType;
import ru.agroeco.pulse.platform.domain.event.DomainEvent;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Builder
public record DepartmentCreated(
        UUID departmentId,
        String code,
        String name,
        UUID actorId,
        Instant occurredAt
) implements DomainEvent {

    @Override public DomainEventType eventType() { return DomainEventType.DEPARTMENT_CREATED; }
    @Override public UUID entityId() { return departmentId; }
    @Override public String entitySchema() { return "ref"; }
    @Override public String entityTable() { return "departments"; }
    @Override public Instant occurredAt() { return occurredAt; }
    @Override public UUID actorId() { return actorId; }

    @Override
    public boolean publishToOutbox() {
        return false;
    }

    @Override
    public Object newDataSnapshot() {
        Map<String, Object> data = new HashMap<>();
        data.put("code", code);
        data.put("name", name);
        return data;
    }
}