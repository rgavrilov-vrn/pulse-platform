package ru.agroeco.pulse.audit.application;

import java.time.Instant;
import java.util.UUID;

public record AuditFilter(
        String action,
        String entitySchema,
        String entityTable,
        UUID entityId,
        UUID actorId,
        String requestId,
        String q,
        Instant from,
        Instant to
) {}