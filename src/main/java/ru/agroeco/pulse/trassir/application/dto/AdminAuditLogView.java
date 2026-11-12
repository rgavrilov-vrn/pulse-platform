package ru.agroeco.pulse.trassir.application.dto;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

public record AdminAuditLogView(
        UUID id,
        String actor,
        String action,
        String target,
        Map<String, Object> details,
        Instant createdAt
) {}