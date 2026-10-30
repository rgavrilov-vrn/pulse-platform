// audit/adapter/http/dto/AuditEventSummary.java
package ru.agroeco.pulse.audit.adapter.http.dto;

import java.time.Instant;
import java.util.UUID;

public record AuditEventSummary(
        UUID id,
        Instant occurredAt,
        UUID actorId,
        String action,
        String entitySchema,
        String entityTable,
        UUID entityId,
        String requestId,
        String sourceIp,   // inet → строка, чтобы не утекал InetAddress в JSON
        String userAgent
) {}