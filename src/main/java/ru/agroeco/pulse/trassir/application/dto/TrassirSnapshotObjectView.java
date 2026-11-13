package ru.agroeco.pulse.trassir.application.dto;

import java.time.Instant;
import java.util.UUID;

public record TrassirSnapshotObjectView(
        UUID id,
        UUID snapshotId,
        String objectKey,
        String objectPayload,
        Instant createdAt
) {}