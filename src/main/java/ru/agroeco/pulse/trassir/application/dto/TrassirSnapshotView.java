package ru.agroeco.pulse.trassir.application.dto;

import java.time.Instant;
import java.util.UUID;

public record TrassirSnapshotView(
        UUID id,
        UUID externalSnapshotId,
        String serverId,
        String streamId,
        Long snapshotSeq,
        Integer totalParts,
        Instant receivedAt,
        Instant completedAt,
        String status,
        Integer retryCount,
        String lastError,
        Instant nextRetryAt
) {}