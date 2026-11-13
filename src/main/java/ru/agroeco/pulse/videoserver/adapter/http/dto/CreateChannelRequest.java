package ru.agroeco.pulse.videoserver.adapter.http.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public record CreateChannelRequest(
        @NotNull UUID cameraId,
        @NotBlank @Size(max = 255) String externalId,
        @NotBlank @Size(max = 255) String name
) {}