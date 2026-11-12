package ru.agroeco.pulse.incident.adapter.http.dto;

import ru.agroeco.pulse.incident.domain.ChecklistItemEntity;

import java.time.Instant;
import java.util.UUID;

public record ChecklistItemAdminView(
        UUID id,
        UUID categoryId,
        String code,
        String name,
        String description,
        boolean requiresImage,
        boolean requiresComment,
        boolean requiresVideo,
        String defaultSeverity,
        String commentTemplate,
        int sortOrder,
        boolean active,
        Instant createdAt,
        Instant updatedAt
) {
    public static ChecklistItemAdminView from(ChecklistItemEntity p) {
        return new ChecklistItemAdminView(
                p.getId(),
                p.getCategoryId(),
                p.getCode(),
                p.getName(),
                p.getDescription(),
                p.isRequiresImage(),
                p.isRequiresComment(),
                p.isRequiresVideo(),
                p.getDefaultSeverity() != null ? p.getDefaultSeverity().name() : null,
                p.getCommentTemplate(),
                p.getSortOrder(),
                p.isActive(),
                p.getCreatedAt(),
                p.getUpdatedAt());
    }
}