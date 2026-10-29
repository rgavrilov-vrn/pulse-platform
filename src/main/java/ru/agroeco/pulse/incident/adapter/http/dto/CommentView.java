package ru.agroeco.pulse.incident.adapter.http.dto;

import ru.agroeco.pulse.incident.domain.CommentType;

import java.time.Instant;
import java.util.UUID;

public record CommentView(
        UUID id,
        UUID incidentId,
        UUID authorId,
        CommentType commentType,
        String body,
        Instant createdAt
) {}