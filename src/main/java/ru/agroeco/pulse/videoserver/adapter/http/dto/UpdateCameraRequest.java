package ru.agroeco.pulse.videoserver.adapter.http.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpdateCameraRequest(
        @NotBlank @Size(max = 255) String name,
        @Size(max = 100) String code,
        @Size(max = 2000) String description
) {}