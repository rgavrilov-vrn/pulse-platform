package ru.agroeco.pulse.incident.adapter.http.dto;

// FIX: в Jackson 3 (Spring Boot 4) аннотации остаются в
// com.fasterxml.jackson.annotation.*, переезжают только core/databind.
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