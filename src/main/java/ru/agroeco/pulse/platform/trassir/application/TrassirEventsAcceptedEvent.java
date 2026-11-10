package ru.agroeco.pulse.platform.trassir.application;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record TrassirEventsAcceptedEvent(
        UUID rawId,
        String serverId,
        UUID batchId,
        Instant receivedAt,
        List<UUID> eventIds
) {
}
