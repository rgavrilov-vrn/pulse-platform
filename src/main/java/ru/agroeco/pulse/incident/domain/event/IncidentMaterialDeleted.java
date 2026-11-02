package ru.agroeco.pulse.incident.domain.event;

import lombok.Builder;
import ru.agroeco.pulse.platform.domain.event.DomainEvent;
import ru.agroeco.pulse.platform.domain.event.DomainEventType;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

@Builder
public record IncidentMaterialDeleted(
        UUID incidentId,
        UUID materialId,
        String fileName,
        UUID actorId,
        Instant occurredAt
) implements DomainEvent {

    @Override public DomainEventType eventType() { return DomainEventType.INCIDENT_MATERIAL_DELETED; }
    @Override public UUID entityId() { return materialId; }
    @Override public String entitySchema() { return "incident"; }
    @Override public String entityTable() { return "media_assets"; }
    @Override public Instant occurredAt() { return occurredAt; }
    @Override public UUID actorId() { return actorId; }
    @Override public Object oldDataSnapshot() {
        return Map.of("fileName", fileName);
    }
}
