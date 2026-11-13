package ru.agroeco.pulse.videoserver.adapter.http.dto;

import ru.agroeco.pulse.videoserver.domain.Camera;
import ru.agroeco.pulse.videoserver.domain.CameraStatus;

import java.time.Instant;
import java.util.UUID;

public record CameraResponse(
        UUID id,
        UUID videoServerId,
        String source,
        String externalId,
        String code,
        String name,
        String description,
        CameraStatus status,
        boolean isActive,
        Instant lastSeenAt,
        Instant deletedAt,
        String deactivatedBy,
        Instant createdAt,
        Instant updatedAt
) {
    public static CameraResponse from(Camera c) {
        return new CameraResponse(
                c.getId(),
                c.getVideoServerId(),
                c.getSource(),
                c.getExternalId(),
                c.getCode(),
                c.getName(),
                c.getDescription(),
                c.getStatus(),
                c.isActive(),
                c.getLastSeenAt(),
                c.getDeletedAt(),
                c.getDeactivatedBy(),
                c.getCreatedAt(),
                c.getUpdatedAt()
        );
    }
}