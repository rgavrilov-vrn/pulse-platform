package ru.agroeco.pulse.trassir.application.dto;

import java.time.Instant;
import java.util.UUID;

public record TrassirSystemEventView(
        UUID id,
        String serverId,
        UUID eventId,
        String eventType,
        String eventVersion,
        Instant occurredAt,
        Instant observedAt,
        String severity,
        String source,
        String data,
        Instant createdAt
) {}