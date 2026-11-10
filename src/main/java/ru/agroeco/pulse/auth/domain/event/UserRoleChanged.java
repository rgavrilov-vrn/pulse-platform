package ru.agroeco.pulse.auth.domain.event;

import lombok.Builder;
import ru.agroeco.pulse.platform.domain.event.DomainEvent;
import ru.agroeco.pulse.platform.domain.event.DomainEventType;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Builder
public record UserRoleChanged(
        UUID userId,
        String login,
        Set<String> oldRoles,
        Set<String> newRoles,
        UUID actorId,
        Instant occurredAt
) implements DomainEvent {

    @Override public DomainEventType eventType() { return DomainEventType.USER_ROLE_CHANGED; }
    @Override public UUID entityId()              { return userId; }
    @Override public String entitySchema()        { return "auth"; }
    @Override public String entityTable()         { return "user_roles"; }
    @Override public Instant occurredAt()         { return occurredAt; }
    @Override public UUID actorId()               { return actorId; }

    @Override public boolean publishToOutbox()    { return false; }

    @Override public Object oldDataSnapshot() {
        Map<String, Object> data = new HashMap<>();
        data.put("login", login);
        data.put("roles", oldRoles);
        return data;
    }

    @Override public Object newDataSnapshot() {
        Map<String, Object> data = new HashMap<>();
        data.put("login", login);
        data.put("roles", newRoles);
        return data;
    }
}