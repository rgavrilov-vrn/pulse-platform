package ru.agroeco.pulse.platform.trassir.application;

import java.time.Instant;
import java.util.UUID;

public record TrassirEventContext(
        String serverId,
        UUID batchId,
        Instant receivedAt,
        ParsedTrassirEnvelope.Event event
) {
}
