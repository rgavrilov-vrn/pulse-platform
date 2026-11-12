package ru.agroeco.pulse.refs.adapter.dto;

import java.time.Instant;
import java.util.UUID;

public record FacilityResponse(
        UUID id,
        String code,
        String name,
        String description,
        String address,
        boolean isActive,
        Instant createdAt,
        Instant updatedAt
) {
}