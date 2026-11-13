package ru.agroeco.pulse.trassir.application.dto;

import java.time.Instant;
import java.util.UUID;

public record TrassirServerView(
        UUID serverId,
        Instant lastHeartbeatAt,
        Instant lastSnapshotAt,
        Instant updatedAt
) {}