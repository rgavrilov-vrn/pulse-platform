package ru.agroeco.pulse.notification.api;

import java.time.Instant;
import java.util.UUID;

public record NotificationRequested(
    UUID notificationId,
    UUID incidentId,
    UUID correlationId,
    String templateCode,
    String priority,
    String channel,
    Recipient recipient,
    boolean acknowledgementRequired,
    Instant ackDeadlineAt,
    Instant occurredAt,
    String payload
) {

    public record Recipient(
        UUID userId,
        String role,
        UUID siteId
    ) {
    }
}
