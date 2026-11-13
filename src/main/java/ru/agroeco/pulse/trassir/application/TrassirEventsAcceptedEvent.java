package ru.agroeco.pulse.trassir.application;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record TrassirEventsAcceptedEvent(
        UUID rawId,
        UUID serverId,
        UUID batchId,
        Instant receivedAt,
        List<UUID> eventIds
) {}