package ru.agroeco.pulse.incident.adapter.http.dto;

import java.time.Instant;
import java.util.UUID;

public record ViolationCategoryAdminView(
        UUID id, UUID groupId, String code, String name,
        String description, int sortOrder, boolean active,
        Instant createdAt, Instant updatedAt
) {}