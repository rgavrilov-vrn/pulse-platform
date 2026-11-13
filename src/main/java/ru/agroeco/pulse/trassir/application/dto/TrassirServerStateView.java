package ru.agroeco.pulse.trassir.application.dto;

import java.time.Instant;
import java.util.UUID;

/** Alias для TrassirServerView; оставлен для читаемости в контроллере. */
public record TrassirServerStateView(
        UUID serverId,
        Instant lastHeartbeatAt,
        Instant lastSnapshotAt,
        Instant updatedAt
) {
    public static TrassirServerStateView from(TrassirServerView v) {
        return new TrassirServerStateView(
                v.serverId(), v.lastHeartbeatAt(), v.lastSnapshotAt(), v.updatedAt());
    }
}