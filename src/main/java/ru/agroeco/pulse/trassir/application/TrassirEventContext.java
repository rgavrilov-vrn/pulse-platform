package ru.agroeco.pulse.trassir.application;

import java.time.Instant;
import java.util.UUID;

public record TrassirEventContext(
        UUID serverId,
        UUID batchId,
        Instant receivedAt,
        ParsedTrassirEnvelope.Event event
) {}