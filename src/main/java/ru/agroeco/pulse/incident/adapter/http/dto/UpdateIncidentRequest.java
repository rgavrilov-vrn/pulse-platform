package ru.agroeco.pulse.incident.adapter.http.dto;

import jakarta.validation.constraints.Size;
import ru.agroeco.pulse.incident.domain.IncidentSeverity;
import ru.agroeco.pulse.incident.domain.command.IncidentCardUpdate;
import ru.agroeco.pulse.incident.domain.command.IncidentCategoryChangeCommand;
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

        /** Каноничное имя (FIX #117). */
        UUID objectId,

        /** @deprecated используйте {@link #objectId()}. */
        @Deprecated
        UUID facilityId,

        Boolean clearFacility,
        Boolean replaceCheckpoints,
        UUID expectedCategoryId,
        Integer expectedVersion,
        @Size(max = 1000) List<UUID> expectedCheckpointIds
) {
    /**
     * Разрешение objectId/facilityId с проверкой конфликта.
     * Используется и в {@link #toCardUpdate()}, и (если понадобится)
     * в других потребителях.
     */
    public UUID resolvedObjectId() {
        if (objectId != null && facilityId != null && !objectId.equals(facilityId)) {
            throw new BusinessRuleException(
                    "Both objectId and facilityId provided and differ; use objectId only");
        }
        return objectId != null ? objectId : facilityId;
    }

    /** Static factory: HTTP-DTO → domain-command. */
    public IncidentCardUpdate toCardUpdate() {
        return new IncidentCardUpdate(
                severity,
                resolvedObjectId(),
                Boolean.TRUE.equals(clearFacility));
    }

    /** Static factory: HTTP-DTO → domain-command. */
    public IncidentCategoryChangeCommand toCategoryChangeCommand() {
        return new IncidentCategoryChangeCommand(
                categoryId,
                Boolean.TRUE.equals(replaceCheckpoints),
                expectedCheckpointIds,
                expectedVersion,
                expectedCategoryId);
    }

    // Backward-compatible constructors
    public UpdateIncidentRequest(UUID categoryId, Instant eventTime, String comment, String checkpoint) {
        this(categoryId,eventTime,comment,checkpoint,null,null,null,null,null,null,null,null);
    }
    public UpdateIncidentRequest(UUID categoryId, Instant eventTime, String comment, String checkpoint,
                                 IncidentSeverity severity, UUID facilityId, Boolean clearFacility) {
        this(categoryId,eventTime,comment,checkpoint,severity,null,facilityId,clearFacility,null,null,null,null);
    }
}