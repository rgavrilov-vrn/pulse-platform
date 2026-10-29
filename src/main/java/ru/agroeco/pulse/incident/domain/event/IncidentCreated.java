package ru.agroeco.pulse.incident.domain.event;

import lombok.Builder;
import ru.agroeco.pulse.audit.domain.event.AuditAction;
import ru.agroeco.pulse.incident.domain.IncidentSeverity;
import ru.agroeco.pulse.incident.domain.IncidentStatus;
import ru.agroeco.pulse.platform.domain.event.DomainEvent;

import java.time.Instant;
import java.util.UUID;

@Builder
public record IncidentCreated(
        UUID incidentId,
        String number,
        String source,
        IncidentStatus status,
        IncidentSeverity severity,
        Instant eventTime,
        UUID actorId,
        Instant occurredAt
) implements DomainEvent {

    @Override public AuditAction eventType() { return AuditAction.INCIDENT_CREATED; }
    @Override public UUID entityId() { return incidentId; }
    @Override public String entitySchema() { return "incident"; }
    @Override public String entityTable() { return "incidents"; }
    @Override public Instant occurredAt() { return occurredAt; }
}