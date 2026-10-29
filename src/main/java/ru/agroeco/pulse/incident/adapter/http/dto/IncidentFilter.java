package ru.agroeco.pulse.incident.adapter.http.dto;

import ru.agroeco.pulse.incident.domain.IncidentSeverity;
import ru.agroeco.pulse.incident.domain.IncidentStatus;

import java.time.Instant;
import java.util.UUID;

public record IncidentFilter(
        IncidentStatus status,
        IncidentSeverity severity,
        String source,
        UUID assignedTo,
        Instant eventFrom,
        Instant eventTo
) {}