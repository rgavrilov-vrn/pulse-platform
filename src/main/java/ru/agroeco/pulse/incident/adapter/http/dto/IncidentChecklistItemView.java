package ru.agroeco.pulse.incident.adapter.http.dto;

import java.time.Instant;
import java.util.UUID;

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