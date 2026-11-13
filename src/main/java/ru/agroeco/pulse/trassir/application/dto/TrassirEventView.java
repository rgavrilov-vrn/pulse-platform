package ru.agroeco.pulse.trassir.application.dto;

import java.time.Instant;
import java.util.UUID;

public record TrassirEventView(
        UUID id,
        String serverId,
        UUID eventId,
        Integer eventIndex,
        String eventType,
        String eventVersion,
        Instant occurredAt,
        Instant observedAt,
        String status,
        Integer retryCount,
        String lastError,
        Instant nextRetryAt,
        Instant createdAt
) {}