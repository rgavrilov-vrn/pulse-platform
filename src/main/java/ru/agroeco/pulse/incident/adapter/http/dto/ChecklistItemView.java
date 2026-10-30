package ru.agroeco.pulse.incident.adapter.http.dto;


import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.UUID;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record ChecklistItemView(
        UUID id,
        UUID categoryId,
        String name,
        String commentTemplate
) {}