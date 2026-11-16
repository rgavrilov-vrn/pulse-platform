package ru.agroeco.pulse.trassir.api.event;

import ru.agroeco.pulse.platform.domain.event.DomainEventType;
import ru.agroeco.pulse.platform.domain.event.IntegrationEvent;

import java.time.Instant;
import java.util.UUID;

public record TrassirManualIncidentRequested(
        UUID eventId,
        int schemaVersion,
        String objectId,
        String objectName,
        String objectClass,
        String comment,
        String reason,
        String videoTimestamp,
        String imageFile,
        Instant occurredAt
) implements IntegrationEvent {

    public TrassirManualIncidentRequested {
        if (schemaVersion <= 0) schemaVersion = 1;
    }

    @Override public DomainEventType eventType() { return DomainEventType.TRASSIR_MANUAL_INCIDENT_REQUESTED; }
    @Override public UUID entityId()              { return eventId; }
    @Override public String entitySchema()        { return "trassir"; }
    @Override public String entityTable()         { return "manual_incidents_raw"; }
    @Override public Instant occurredAt()         { return occurredAt; }
    @Override public boolean publishToOutbox()    { return false; }
}