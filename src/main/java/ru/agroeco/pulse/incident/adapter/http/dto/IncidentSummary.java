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
        IncidentKind kind,
        Instant eventTime,
        Instant createdAt,
        Instant updatedAt,
        UUID assignedTo,
        String comment,
        UUID categoryId,
        String categoryName,
        String siteName,
        String authorName
) {
    public IncidentSummary(UUID id, String number, String source,
            IncidentCreationMode creationMode, IncidentStatus status,
            IncidentSeverity severity, IncidentKind kind, Instant eventTime,
            Instant createdAt, Instant updatedAt, UUID assignedTo, String comment,
            UUID categoryId, String categoryName) {
        this(id,number,source,creationMode,status,severity,kind,eventTime,createdAt,
                updatedAt,assignedTo,comment,categoryId,categoryName,null,null);
    }
}
