package ru.agroeco.pulse.refs.adapter.dto;

import java.time.Instant;
import java.util.UUID;

public record EmployeeResponse(
        UUID id,
        String personnelNo,
        String fullName,
        UUID departmentId,
        boolean isActive,
        Instant createdAt,
        Instant updatedAt
) {
}