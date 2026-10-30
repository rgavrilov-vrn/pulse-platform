package ru.agroeco.pulse.videoserver.api;

import java.time.Instant;
import java.util.UUID;

public record VideoServerResponse(
        UUID id,
        UUID objectId,
        UUID zoneId,
        String source,
        String externalId,
        String name,
        String host,
        String baseUrl,
        String description,
        String status,
        boolean isActive,
        Instant createdAt,
        Instant updatedAt
) {
}