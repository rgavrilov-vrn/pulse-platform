package ru.agroeco.pulse.incidentorchestrator.application;


import java.time.Instant;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.agroeco.pulse.incident.api.IncidentCreated;
import ru.agroeco.pulse.incidentorchestrator.api.IncidentOrchestratorProperties;
import ru.agroeco.pulse.notification.api.NotificationRequested;
import ru.agroeco.pulse.outbox.api.OutboxMessage;
import ru.agroeco.pulse.outbox.api.OutboxPort;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

@Service
@RequiredArgsConstructor
public class RouteIncidentUseCase {

    public static final String NOTIFICATION_REQUESTED = "NotificationRequested";

    private final IncidentOrchestratorProperties properties;
    private final OutboxPort outboxPort;
    private final ObjectMapper objectMapper;

    @Transactional
    public NotificationRequested handle(IncidentCreated event) {
        boolean critical = "CRITICAL".equalsIgnoreCase(event.severity());
        String channel = critical
            ? properties.getCriticalChannel()
            : properties.getDefaultChannel();
        String priority = critical ? "HIGH" : "NORMAL";
        String template = critical
            ? "critical-incident-created"
            : "incident-created";
        boolean acknowledgementRequired = critical;
        Instant ackDeadline = critical
            ? Instant.now().plus(properties.getCriticalReactionSla())
            : Instant.now().plus(properties.getDefaultReactionSla());

        NotificationRequested request = new NotificationRequested(
            UUID.randomUUID(),
            event.incidentId(),
            event.incidentId(),
            template,
            priority,
            channel,
            new NotificationRequested.Recipient(
                properties.getDefaultRecipientId(),
                properties.getDefaultRole(),
                null
            ),
            acknowledgementRequired,
            ackDeadline,
            event.occurredAt(),
            buildPayload(event)
        );

        publish(request);
        return request;
    }

    private void publish(NotificationRequested request) {
        try {
            String payload = objectMapper.writeValueAsString(request);
            outboxPort.publish(new OutboxMessage(
                request.notificationId(),
                NOTIFICATION_REQUESTED,
                request.incidentId(),
                payload,
                request.occurredAt()
            ));
        } catch (JacksonException exception) {
            throw new IllegalStateException("Failed to serialize NotificationRequested", exception);
        }
    }

    private String buildPayload(IncidentCreated event) {
        try {
            return objectMapper.writeValueAsString(event);
        } catch (JacksonException exception) {
            throw new IllegalStateException("Failed to serialize incident payload", exception);
        }
    }
}
