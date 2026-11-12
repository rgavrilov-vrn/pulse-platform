package ru.agroeco.pulse.refs.adapter.dto;

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