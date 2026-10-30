package ru.agroeco.pulse.incident.adapter.http.dto;

import java.util.UUID;

public record CategoryView(
        UUID id,
        UUID groupId,
        String code,
        String name
) {}