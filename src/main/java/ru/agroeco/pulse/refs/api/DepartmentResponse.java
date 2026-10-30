package ru.agroeco.pulse.refs.api;

import java.time.Instant;
import java.util.UUID;

public record DepartmentResponse(
        UUID id,
        String code,
        String name,
        boolean isActive,
        Instant createdAt,
        Instant updatedAt
) {
}