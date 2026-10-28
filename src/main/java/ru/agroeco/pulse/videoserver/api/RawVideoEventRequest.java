package ru.agroeco.pulse.videoserver.api;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.Instant;
import java.util.UUID;

public record RawVideoEventRequest(
    @NotNull UUID videoServerId,
    UUID cameraId,
    UUID channelId,
    @NotNull Instant occurredAt,
    @NotBlank String source,
    @NotBlank String rawPayload
) {
}
