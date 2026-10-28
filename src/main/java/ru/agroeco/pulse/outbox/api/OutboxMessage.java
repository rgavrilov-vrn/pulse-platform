package ru.agroeco.pulse.outbox.api;

import java.time.Instant;
import java.util.UUID;

public record OutboxMessage(
    UUID eventId,
    String eventType,
    UUID entityId,
    String payload,
    Instant occurredAt
) {
    public OutboxMessage {
        if (eventId == null) {
            eventId = UUID.randomUUID();
        }
        if (occurredAt == null) {
            occurredAt = Instant.now();
        }
    }
}
