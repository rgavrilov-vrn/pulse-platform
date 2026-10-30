package ru.agroeco.pulse.incident.adapter.http.dto;



import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.UUID;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record CategoryGroupView(
        UUID id,
        String code,
        String name,
        String icon,
        int sortOrder
) {}