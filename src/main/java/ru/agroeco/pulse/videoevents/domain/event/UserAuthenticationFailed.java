package ru.agroeco.pulse.videoevents.domain.event;

import java.time.Instant;
import java.util.UUID;

public record UserAuthenticationFailed(
    String username,
    String reason,
    Instant occurredAt
) implements DomainEvent {

    @Override
    public String eventType() {
        return "USER_AUTHENTICATION_FAILED";
    }

    @Override
    public UUID entityId() {
        return null;
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
