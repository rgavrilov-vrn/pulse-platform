package ru.agroeco.pulse.videoserver.adapter.http.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public record UpdateVideoServerRequest(
        @NotBlank @Size(max = 255) String name,
        @Size(max = 255) String host,
        @Size(max = 512) String baseUrl,
        @Size(max = 2000) String description,
        UUID objectId,
        UUID zoneId,
        Boolean isActive
) {}