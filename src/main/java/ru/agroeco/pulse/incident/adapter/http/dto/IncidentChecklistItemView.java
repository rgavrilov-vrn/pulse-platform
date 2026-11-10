package ru.agroeco.pulse.incident.adapter.http.dto;

// FIX: аннотации Jackson 3 остаются в com.fasterxml.jackson.annotation.*
import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.Instant;
import java.util.UUID;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record IncidentChecklistItemView(
        UUID id,
        UUID incidentId,
        UUID checklistItemId,
        String itemName,
        String categoryName,
        String severity,
        String comment,
        UUID createdBy,
        Instant createdAt
) {}