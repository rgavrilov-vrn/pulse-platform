package ru.agroeco.pulse.incident.domain.event;

import lombok.Builder;
import ru.agroeco.pulse.audit.domain.event.AuditAction;
import ru.agroeco.pulse.incident.domain.CommentType;
import ru.agroeco.pulse.platform.domain.event.DomainEvent;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

@Builder
public record IncidentCommentAdded(
        UUID incidentId,
        UUID commentId,
        UUID authorId,
        CommentType commentType,
        Instant occurredAt
) implements DomainEvent {

    @Override public AuditAction eventType() { return AuditAction.INCIDENT_COMMENT_ADDED; }
    @Override public UUID entityId() { return incidentId; }  // аудит привязан к инциденту
    @Override public String entitySchema() { return "incident"; }
    @Override public String entityTable() { return "comments"; }
    @Override public Instant occurredAt() { return occurredAt; }
    @Override public UUID actorId() { return authorId; }
    @Override public Object newDataSnapshot() {
        return Map.of("commentId", commentId.toString(), "commentType", commentType.name());
    }
}