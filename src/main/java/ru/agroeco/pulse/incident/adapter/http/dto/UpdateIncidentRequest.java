package ru.agroeco.pulse.incident.adapter.http.dto;

import jakarta.validation.constraints.Size;
import ru.agroeco.pulse.incident.domain.IncidentSeverity;
import java.time.Instant;
import java.util.UUID;

public record UpdateIncidentRequest(
        UUID categoryId,
        Instant eventTime,
        @Size(max = 10_000) String comment,
        @Size(max = 100) String checkpoint,
        IncidentSeverity severity,
        UUID facilityId,
        Boolean clearFacility
) {
    public UpdateIncidentRequest(UUID categoryId, Instant eventTime, String comment, String checkpoint) {
        this(categoryId,eventTime,comment,checkpoint,null,null,null);
    }
}
