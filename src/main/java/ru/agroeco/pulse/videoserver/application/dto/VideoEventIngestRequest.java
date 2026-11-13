package ru.agroeco.pulse.videoserver.application.dto;

import java.time.Instant;
import java.util.UUID;

/**
 * Внутренний DTO для {@link ru.agroeco.pulse.videoserver.application.IngestVideoEventUseCase}.
 * Формируется из {@code TrassirVideoEventReceived} (videoserver.adapter.messaging).
 * Не является HTTP-контрактом.
 */
public record VideoEventIngestRequest(
        UUID videoServerId,
        UUID sourceEventId,        // = trassir.events.event_id
        String externalEventId,    // idempotency key
        UUID cameraId,
        UUID channelId,
        Instant occurredAt,
        String source,             // "trassir"
        String subtype,            // MOTION | OFFLINE | ...
        String severity            // INFO | WARNING | CRITICAL
) {}