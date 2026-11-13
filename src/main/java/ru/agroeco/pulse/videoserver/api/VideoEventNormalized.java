package ru.agroeco.pulse.videoserver.api;

import java.time.Instant;
import java.util.UUID;

public record VideoEventNormalized(
        UUID eventId,
        UUID sourceEventId,        // = trassir.events.event_id (traceability)
        UUID videoServerId,
        UUID cameraId,
        UUID channelId,
        UUID objectId,             // reserved: facility link (future)
        Instant occurredAt,
        String source,
        VideoEventType eventType,
        EventSeverity severity,
        int payloadVersion
) {}