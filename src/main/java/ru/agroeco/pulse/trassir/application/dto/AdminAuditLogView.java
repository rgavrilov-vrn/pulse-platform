package ru.agroeco.pulse.trassir.application.dto;

import java.time.Instant;
import java.util.UUID;

public record AdminAuditLogView(
        UUID id,
        String actor,
        String action,
        String target,
        String details,
        Instant createdAt
) {}