package ru.agroeco.pulse.incident.domain.event;

import ru.agroeco.pulse.platform.domain.event.DomainEvent;
import ru.agroeco.pulse.platform.domain.event.DomainEventType;
import java.time.Instant;
import java.util.UUID;

public record IncidentCatalogChanged(DomainEventType eventType,String entityTable,
        UUID entityId,UUID actorId,Instant occurredAt,Object oldDataSnapshot,Object newDataSnapshot) implements DomainEvent {
    @Override public String entitySchema() { return "incident"; }
    @Override public boolean publishToOutbox() { return false; }
}
