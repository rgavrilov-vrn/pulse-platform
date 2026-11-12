package ru.agroeco.pulse.incident.domain.command;

import ru.agroeco.pulse.incident.domain.IncidentSeverity;

import java.util.UUID;

public record IncidentCardUpdate(
        IncidentSeverity severity,
        UUID objectId,
        boolean clearFacility
) {
    public boolean isEmpty() {
        return severity == null && objectId == null && !clearFacility;
    }
}