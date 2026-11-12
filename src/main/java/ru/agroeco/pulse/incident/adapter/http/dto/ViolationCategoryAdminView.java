package ru.agroeco.pulse.incident.adapter.http.dto;

import ru.agroeco.pulse.incident.domain.ViolationCategoryEntity;

import java.time.Instant;
import java.util.UUID;

public record ViolationCategoryAdminView(
        UUID id,
        UUID groupId,
        String code,
        String name,
        String description,
        int sortOrder,
        boolean active,
        Instant createdAt,
        Instant updatedAt
) {
    /**
     * Static factory: entity → view.
     * Adapter-слой (dto) законно зависит от domain; обратное — нет.
     */
    public static ViolationCategoryAdminView from(ViolationCategoryEntity c) {
        return new ViolationCategoryAdminView(
                c.getId(),
                c.getGroupId(),
                c.getCode(),
                c.getName(),
                c.getDescription(),
                c.getSortOrder(),
                c.isActive(),
                c.getCreatedAt(),
                c.getUpdatedAt());
    }
}