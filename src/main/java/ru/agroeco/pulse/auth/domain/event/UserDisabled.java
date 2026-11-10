package ru.agroeco.pulse.auth.domain.event;

import lombok.Builder;
import ru.agroeco.pulse.platform.domain.event.DomainEvent;
import ru.agroeco.pulse.platform.domain.event.DomainEventType;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

@Builder
public record UserDisabled(
        UUID userId,
        String login,
        UUID actorId,
        Instant occurredAt
) implements DomainEvent {

    @Override public DomainEventType eventType() { return DomainEventType.USER_DISABLED; }
    @Override public UUID entityId()              { return userId; }
    @Override public String entitySchema()        { return "auth"; }
    @Override public String entityTable()         { return "users"; }
    @Override public Instant occurredAt()         { return occurredAt; }
    @Override public UUID actorId()               { return actorId; }

    @Override public boolean publishToOutbox()    { return false; }

    @Override public Object oldDataSnapshot() {
        return Map.of("login", login, "enabled", true);
    }

    @Override public Object newDataSnapshot() {
        return Map.of("login", login, "enabled", false);
    }
}