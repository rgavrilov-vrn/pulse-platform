package ru.agroeco.pulse.videoserver.api;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import ru.agroeco.pulse.videoserver.api.validation.ValidJson;

import java.time.Instant;
import java.util.UUID;

public record RawVideoEventRequest(
        @NotNull UUID videoServerId,
        UUID cameraId,
        UUID channelId,
        @NotBlank @Size(max = 200) String externalEventId,
        @NotNull Instant occurredAt,
        @NotBlank @Size(max = 64) String source,   // было max = 100 — БД: VARCHAR(64)
        @NotBlank @ValidJson String rawPayload
) {
}