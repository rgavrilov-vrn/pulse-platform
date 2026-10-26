package ru.agroeco.pulse.videoevents.domain.event;

import java.time.Instant;
import java.util.UUID;

public record UserAuthenticated(
    UUID userId,
    String username,
    Instant occurredAt
) implements DomainEvent {

    @Override
    public String eventType() {
        return "USER_AUTHENTICATED";
    }

    @Override
    public UUID entityId() {
        return userId;
    }

    @Override
    public String entitySchema() {
        return "auth";
    }

    @Override
    public String entityTable() {
        return "users";
    }
}
