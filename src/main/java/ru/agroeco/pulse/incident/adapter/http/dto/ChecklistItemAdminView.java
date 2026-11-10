package ru.agroeco.pulse.incident.adapter.http.dto;

import java.time.Instant;
import java.util.UUID;

public record ChecklistItemAdminView(
        UUID id, UUID categoryId, String code, String name, String description,
        boolean requiresImage, boolean requiresComment, boolean requiresVideo,
        String defaultSeverity, String commentTemplate, int sortOrder, boolean active,
        Instant createdAt, Instant updatedAt
) {}