package ru.agroeco.pulse.incident.adapter.http.dto;

import jakarta.validation.constraints.Size;

import java.time.Instant;
import java.util.UUID;

public record UpdateIncidentRequest(
        UUID categoryId,
        Instant eventTime,
        @Size(max = 10_000) String comment,
        @Size(max = 100) String checkpoint
) {}