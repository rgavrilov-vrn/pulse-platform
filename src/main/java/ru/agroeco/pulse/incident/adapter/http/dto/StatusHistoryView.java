package ru.agroeco.pulse.incident.adapter.http.dto;

import ru.agroeco.pulse.incident.domain.IncidentStatus;

import java.time.Instant;
import java.util.UUID;

public record StatusHistoryView(
        Long id,
        UUID incidentId,
        IncidentStatus fromStatus,
        IncidentStatus toStatus,
        UUID changedBy,
        Instant changedAt,
        String comment,
        String requestId
) {
}