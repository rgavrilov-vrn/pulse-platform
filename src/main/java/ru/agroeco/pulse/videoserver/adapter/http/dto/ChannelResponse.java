package ru.agroeco.pulse.videoserver.adapter.http.dto;

import ru.agroeco.pulse.videoserver.domain.Channel;
import ru.agroeco.pulse.videoserver.domain.ChannelStatus;

import java.time.Instant;
import java.util.UUID;

public record ChannelResponse(
        UUID id,
        UUID cameraId,
        String externalId,
        String name,
        ChannelStatus status,
        boolean isActive,
        Instant lastSeenAt,
        Instant createdAt,
        Instant updatedAt
) {
    public static ChannelResponse from(Channel c) {
        return new ChannelResponse(
                c.getId(),
                c.getCameraId(),
                c.getExternalId(),
                c.getName(),
                c.getStatus(),
                c.isActive(),
                c.getLastSeenAt(),
                c.getCreatedAt(),
                c.getUpdatedAt()
        );
    }
}