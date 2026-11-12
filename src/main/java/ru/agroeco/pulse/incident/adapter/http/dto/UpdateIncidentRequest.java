package ru.agroeco.pulse.incident.adapter.http.dto;

import jakarta.validation.constraints.Size;
import ru.agroeco.pulse.incident.domain.IncidentSeverity;
import ru.agroeco.pulse.platform.domain.exception.BusinessRuleException;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record UpdateIncidentRequest(
        UUID categoryId,
        Instant eventTime,
        @Size(max = 10_000) String comment,
        @Size(max = 100) String checkpoint,
        IncidentSeverity severity,

        /** FIX #117: каноничное имя. */
        UUID objectId,

        /** @deprecated используйте {@link #objectId()}. Оставлено для обратной совместимости клиентов. */
        @Deprecated
        UUID facilityId,

        Boolean clearFacility,
        Boolean replaceCheckpoints,
        UUID expectedCategoryId,
        Integer expectedVersion,
        @Size(max=1000) List<UUID> expectedCheckpointIds
) {
    /**
     * FIX #117: resolved Object ID. Приоритет — objectId; facilityId как fallback
     * для старых клиентов. Если оба заданы и различаются — 422.
     */
    public UUID resolvedObjectId() {
        if (objectId != null && facilityId != null && !objectId.equals(facilityId)) {
            throw new BusinessRuleException(
                    "Both objectId and facilityId provided and differ; use objectId only");
        }
        return objectId != null ? objectId : facilityId;
    }

    // Backward-compatible constructors (FacilityId теперь на позиции 7)
    public UpdateIncidentRequest(UUID categoryId, Instant eventTime, String comment, String checkpoint) {
        this(categoryId,eventTime,comment,checkpoint,null,null,null,null,null,null,null,null);
    }
    public UpdateIncidentRequest(UUID categoryId, Instant eventTime, String comment, String checkpoint,
                                 IncidentSeverity severity, UUID facilityId, Boolean clearFacility) {
        this(categoryId,eventTime,comment,checkpoint,severity,null,facilityId,clearFacility,null,null,null,null);
    }
}