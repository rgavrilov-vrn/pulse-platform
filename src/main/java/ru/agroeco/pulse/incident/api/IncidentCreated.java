package ru.agroeco.pulse.incident.api;

import java.time.Instant;
import java.util.UUID;

public record IncidentCreated(
    UUID incidentId,
    UUID sourceEventId,
    String sourceEventType,
    UUID videoServerId,
    UUID cameraId,
    UUID channelId,
    UUID objectId,
    String severity,
    String title,
    Instant occurredAt
) {
}
