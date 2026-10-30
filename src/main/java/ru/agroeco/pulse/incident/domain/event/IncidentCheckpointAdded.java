package ru.agroeco.pulse.incident.domain.event;

import lombok.Builder;
import ru.agroeco.pulse.incident.domain.IncidentKind;
import ru.agroeco.pulse.platform.domain.event.DomainEvent;
import ru.agroeco.pulse.platform.domain.event.DomainEventType;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

@Builder
public record IncidentCheckpointAdded(
        UUID incidentId,
        UUID linkId,
        UUID checklistItemId,
        String checklistItemCode,
        IncidentKind kind,
        UUID actorId,
        Instant occurredAt
) implements DomainEvent {

    @Override public DomainEventType eventType() { return DomainEventType.INCIDENT_CHECKPOINT_ADDED; }
    @Override public UUID entityId() { return incidentId; }
    @Override public String entitySchema() { return "incident"; }
    @Override public String entityTable() { return "incident_checklist_items"; }
    @Override public Instant occurredAt() { return occurredAt; }
    @Override public UUID actorId() { return actorId; }
    @Override public Object newDataSnapshot() {
        return Map.of(
                "linkId", linkId.toString(),
                "checklistItemId", checklistItemId.toString(),
                "checklistItemCode", checklistItemCode,
                "kind", kind.name());
    }
}