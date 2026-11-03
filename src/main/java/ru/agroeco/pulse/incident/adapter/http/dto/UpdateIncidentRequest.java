package ru.agroeco.pulse.incident.adapter.http.dto;

import jakarta.validation.constraints.Size;
import ru.agroeco.pulse.incident.domain.IncidentSeverity;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record UpdateIncidentRequest(
        UUID categoryId,
        Instant eventTime,
        @Size(max = 10_000) String comment,
        @Size(max = 100) String checkpoint,
        IncidentSeverity severity,
        UUID facilityId,
        Boolean clearFacility,
        Boolean replaceCheckpoints,
        UUID expectedCategoryId,
        Integer expectedVersion,
        @Size(max=1000) List<UUID> expectedCheckpointIds
) {
    public UpdateIncidentRequest(UUID categoryId, Instant eventTime, String comment, String checkpoint) {
        this(categoryId,eventTime,comment,checkpoint,null,null,null,null,null,null,null);
    }
    public UpdateIncidentRequest(UUID categoryId, Instant eventTime, String comment, String checkpoint,
            IncidentSeverity severity, UUID facilityId, Boolean clearFacility) {
        this(categoryId,eventTime,comment,checkpoint,severity,facilityId,clearFacility,null,null,null,null);
    }
}
