package ru.agroeco.pulse.incident.domain.command;

import java.util.List;
import java.util.UUID;

public record IncidentCategoryChangeCommand(
        UUID newCategoryId,
        boolean replaceCheckpoints,
        List<UUID> expectedCheckpointIds,
        Integer expectedVersion,
        UUID expectedCategoryId
) {}