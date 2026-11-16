package ru.agroeco.pulse.trassir.application.dto;

import java.time.Instant;
import java.util.UUID;

public record TrassirIngestLogView(
        UUID id,
        Instant receivedAt,
        Integer durationMs,

        String httpMethod,
        String requestUri,
        String remoteAddr,
        String xForwardedFor,
        String contentType,
        Long contentLength,
        String userAgent,
        Boolean authTokenPresent,
        String correlationId,

        String requestBodyPrefix,
        Long requestBodyBytes,
        String requestBodySha256,
        Boolean requestBodyTruncated,
        Boolean requestBodyRead,

        Integer httpStatus,
        String responseCode,
        String responseMessage,
        String responseBody,

        UUID batchId,
        UUID rawId,
        UUID serverId,
        Integer eventCount,

        Instant createdAt
) {}