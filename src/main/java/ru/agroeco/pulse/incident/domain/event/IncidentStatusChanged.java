package ru.agroeco.pulse.incident.domain.event;

import lombok.Builder;
import ru.agroeco.pulse.platform.domain.event.DomainEventType;
import ru.agroeco.pulse.incident.domain.IncidentStatus;
import ru.agroeco.pulse.platform.domain.event.DomainEvent;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

@Builder
public record IncidentStatusChanged(
        UUID incidentId,
        IncidentStatus fromStatus,
        IncidentStatus toStatus,
        String comment,
        UUID actorId,
        Instant occurredAt
) implements DomainEvent {

    @Override public DomainEventType eventType() { return DomainEventType.INCIDENT_STATUS_CHANGED; }
    @Override public UUID entityId() { return incidentId; }
    @Override public String entitySchema() { return "incident"; }
    @Override public String entityTable() { return "incidents"; }
    @Override public Instant occurredAt() { return occurredAt; }
    @Override public UUID actorId() { return actorId; }
    @Override public Object oldDataSnapshot() { return Map.of("status", fromStatus.name()); }
    @Override public Object newDataSnapshot() {
        return Map.of("status", toStatus.name(), "comment", comment == null ? "" : comment);
    }
}