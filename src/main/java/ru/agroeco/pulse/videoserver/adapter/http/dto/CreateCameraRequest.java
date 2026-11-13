package ru.agroeco.pulse.videoserver.adapter.http.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public record CreateCameraRequest(
        @NotNull UUID videoServerId,
        @NotBlank @Size(max = 255) String externalId,
        @Size(max = 100) String code,
        @NotBlank @Size(max = 255) String name,
        String description
) {}