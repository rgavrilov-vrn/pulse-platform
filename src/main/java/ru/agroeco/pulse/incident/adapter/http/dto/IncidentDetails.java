package ru.agroeco.pulse.incident.adapter.http.dto;

import ru.agroeco.pulse.incident.domain.*;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

public record IncidentDetails(
        UUID id,
        String number,
        Integer version,
        String source,
        IncidentCreationMode creationMode,
        IncidentStatus status,
        IncidentSeverity severity,
        Instant eventTime,
        Instant receivedAt,
        Instant createdAt,
        Instant updatedAt,
        UUID createdBy,
        UUID updatedBy,
        UUID operatorId,
        UUID assignedTo,
        Instant dueAt,
        Instant confirmedAt,
        UUID confirmedBy,
        Instant closedAt,
        UUID closedBy,
        String resolutionComment,
        Instant cancelledAt,
        UUID cancelledBy,
        String cancellationReason,
        String comment,
        Map<String, Object> metadata
) {}