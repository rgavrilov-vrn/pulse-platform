package ru.agroeco.pulse.incident.adapter.http.dto;

import ru.agroeco.pulse.incident.domain.*;

import java.time.Instant;
import java.util.UUID;

public record IncidentSummary(
        UUID id,
        String number,
        String source,
        IncidentCreationMode creationMode,
        IncidentStatus status,
        IncidentSeverity severity,
        Instant eventTime,
        Instant createdAt,
        Instant updatedAt,
        UUID assignedTo,
        String comment
) {}