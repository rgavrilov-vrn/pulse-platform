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
public record UserCreated(
        UUID userId,
        String login,
        String displayName,
        Set<String> roles,
        UUID actorId,
        Instant occurredAt
) implements DomainEvent {

    @Override public DomainEventType eventType() { return DomainEventType.USER_CREATED; }
    @Override public UUID entityId()              { return userId; }
    @Override public String entitySchema()        { return "auth"; }
    @Override public String entityTable()         { return "users"; }
    @Override public Instant occurredAt()         { return occurredAt; }
    @Override public UUID actorId()               { return actorId; }

    @Override public boolean publishToOutbox()    { return false; }  // auth-only, только audit

    @Override public Object newDataSnapshot() {
        Map<String, Object> data = new HashMap<>();
        data.put("login", login);
        data.put("displayName", displayName);
        data.put("roles", roles);
        return data;
    }
}