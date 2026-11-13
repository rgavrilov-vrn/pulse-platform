package ru.agroeco.pulse.videoserver.adapter.http.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpdateChannelRequest(
        @NotBlank @Size(max = 255) String name
) {}