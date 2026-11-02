package ru.agroeco.pulse.incident.domain.event;

import lombok.Builder;
import lombok.Value;
import ru.agroeco.pulse.platform.domain.event.DomainEvent;
import ru.agroeco.pulse.platform.domain.event.DomainEventType;

import java.time.Instant;
import java.util.UUID;

@Value
@Builder
public class IncidentMaterialDownloaded implements DomainEvent {
    UUID incidentId;
    UUID materialId;
    String fileName;
    UUID actorId;
    Instant occurredAt;

    @Override
    public DomainEventType eventType() {
        return DomainEventType.INCIDENT_MATERIAL_DOWNLOADED;
    }

    @Override
    public UUID entityId() {
        return materialId;
    }

    @Override
    public String entitySchema() {
        return "incident";
    }

    @Override
    public String entityTable() {
        return "media_assets";
    }
}
