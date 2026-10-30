package ru.agroeco.pulse.refs.api;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public record CreateZoneRequest(
        @NotNull UUID objectId,               // FK → ref.objects
        UUID parentId,                        // null — корневая зона объекта
        @NotBlank @Size(max = 100) String code,
        @NotBlank @Size(max = 255) String name,
        String description,
        Boolean isActive
) {
}