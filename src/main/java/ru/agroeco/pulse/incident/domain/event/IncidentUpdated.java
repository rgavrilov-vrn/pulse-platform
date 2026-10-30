package ru.agroeco.pulse.incident.domain.event;

import lombok.Builder;
import ru.agroeco.pulse.incident.domain.IncidentKind;
import ru.agroeco.pulse.platform.domain.event.DomainEvent;
import ru.agroeco.pulse.platform.domain.event.DomainEventType;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Builder
public record IncidentUpdated(
        UUID incidentId,
        IncidentKind kind,
        Instant eventTime,
        String comment,
        Map<String, Object> oldData,
        UUID actorId,
        Instant occurredAt
) implements DomainEvent {

    @Override public DomainEventType eventType() { return DomainEventType.INCIDENT_UPDATED; }
    @Override public UUID entityId() { return incidentId; }
    @Override public String entitySchema() { return "incident"; }
    @Override public String entityTable() { return "incidents"; }
    @Override public Instant occurredAt() { return occurredAt; }
    @Override public UUID actorId() { return actorId; }
    @Override public Object oldDataSnapshot() { return oldData == null ? Map.of() : oldData; }

    @Override
    public Object newDataSnapshot() {
        Map<String, Object> data = new HashMap<>();
        if (kind      != null) data.put("kind", kind.name());
        if (eventTime != null) data.put("eventTime", eventTime.toString());
        if (comment   != null) data.put("comment", comment);
        return data;
    }
}