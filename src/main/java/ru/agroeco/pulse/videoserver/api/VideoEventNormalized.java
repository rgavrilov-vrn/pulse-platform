package ru.agroeco.pulse.videoserver.api;

import ru.agroeco.pulse.videoserver.domain.EventSeverity;
import ru.agroeco.pulse.videoserver.domain.VideoEventType;

import java.time.Instant;
import java.util.UUID;

public record VideoEventNormalized(
        UUID eventId,
        UUID rawEventId,
        UUID videoServerId,
        UUID cameraId,
        UUID channelId,
        UUID objectId,
        Instant occurredAt,
        String source,
        VideoEventType eventType,
        EventSeverity severity,
        int payloadVersion
) {
}