package ru.agroeco.pulse.refs.adapter.dto;

import java.time.Instant;
import java.util.UUID;

public record ZoneResponse(
        UUID id,
        UUID objectId,
        UUID parentId,
        String code,
        String name,
        String description,
        boolean isActive,
        Instant createdAt,
        Instant updatedAt
) {
}