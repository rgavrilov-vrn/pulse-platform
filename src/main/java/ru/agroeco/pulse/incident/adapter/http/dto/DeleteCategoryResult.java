package ru.agroeco.pulse.incident.adapter.http.dto;

import ru.agroeco.pulse.incident.domain.ViolationCategoryEntity;

import java.util.List;
import java.util.UUID;

public record DeleteCategoryResult(
        UUID id,
        UUID groupId,
        String code,
        String name,
        boolean active,
        List<UUID> deletedCheckpointIds,
        int deletedCheckpointCount
) {
    public static DeleteCategoryResult from(ViolationCategoryEntity c,
                                            List<UUID> deletedCheckpointIds) {
        return new DeleteCategoryResult(
                c.getId(),
                c.getGroupId(),
                c.getCode(),
                c.getName(),
                c.isActive(),
                List.copyOf(deletedCheckpointIds),
                deletedCheckpointIds.size());
    }
}