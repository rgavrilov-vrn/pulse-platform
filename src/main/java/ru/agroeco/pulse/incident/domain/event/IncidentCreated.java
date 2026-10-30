package ru.agroeco.pulse.incident.domain.event;

import lombok.Builder;
import ru.agroeco.pulse.platform.domain.event.DomainEventType;
import ru.agroeco.pulse.incident.domain.IncidentSeverity;
import ru.agroeco.pulse.incident.domain.IncidentStatus;
import ru.agroeco.pulse.platform.domain.event.DomainEvent;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
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
        Instant occurredAt,
        // интеграционный контекст (nullable для ручного пути)
        UUID sourceEventId,
        String sourceEventType,
        UUID videoServerId,
        UUID cameraId,
        UUID channelId,
        UUID objectId,
        String title
) implements DomainEvent {

    @Override public DomainEventType eventType() { return DomainEventType.INCIDENT_CREATED; }
    @Override public UUID entityId() { return incidentId; }
    @Override public String entitySchema() { return "incident"; }
    @Override public String entityTable() { return "incidents"; }
    @Override public Instant occurredAt() { return occurredAt; }
    @Override public UUID actorId() { return actorId; }

    @Override public String outboxEventType() { return "IncidentCreated"; }

    @Override
    public Object newDataSnapshot() {
        Map<String, Object> data = new HashMap<>();
        data.put("number", number);
        data.put("source", source);
        data.put("status", status == null ? null : status.name());
        data.put("severity", severity == null ? null : severity.name());
        return data;
    }
}