package ru.agroeco.pulse.incident.adapter.http.dto;

import ru.agroeco.pulse.incident.domain.*;
import java.time.Instant;
import java.util.List;
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
        String authorName,
        List<String> checkpointNames
) {
    public IncidentSummary {
        checkpointNames=checkpointNames==null?List.of():List.copyOf(checkpointNames);
    }
    public IncidentSummary(UUID id, String number, String source,
            IncidentCreationMode creationMode, IncidentStatus status,
            IncidentSeverity severity, IncidentKind kind, Instant eventTime,
            Instant createdAt, Instant updatedAt, UUID assignedTo, String comment,
            UUID categoryId, String categoryName, String siteName, String authorName) {
        this(id,number,source,creationMode,status,severity,kind,eventTime,createdAt,
                updatedAt,assignedTo,comment,categoryId,categoryName,siteName,authorName,List.of());
    }
    public IncidentSummary(UUID id, String number, String source,
            IncidentCreationMode creationMode, IncidentStatus status,
            IncidentSeverity severity, IncidentKind kind, Instant eventTime,
            Instant createdAt, Instant updatedAt, UUID assignedTo, String comment,
            UUID categoryId, String categoryName) {
        this(id,number,source,creationMode,status,severity,kind,eventTime,createdAt,
                updatedAt,assignedTo,comment,categoryId,categoryName,null,null,List.of());
    }
}
