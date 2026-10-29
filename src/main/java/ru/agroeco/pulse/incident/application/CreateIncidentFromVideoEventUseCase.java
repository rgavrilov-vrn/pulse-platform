package ru.agroeco.pulse.incident.application;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.agroeco.pulse.incident.api.IncidentCreated;
import ru.agroeco.pulse.incident.domain.Incident;
import ru.agroeco.pulse.incident.infrastructure.persistence.IncidentRepository;
import ru.agroeco.pulse.outbox.api.OutboxMessage;
import ru.agroeco.pulse.outbox.api.OutboxPort;
import ru.agroeco.pulse.videoserver.api.VideoEventNormalized;

@Service
@RequiredArgsConstructor
public class CreateIncidentFromVideoEventUseCase {

    public static final String INCIDENT_CREATED = "IncidentCreated";

    private final IncidentRepository incidentRepository;
    private final OutboxPort outboxPort;
    private final ObjectMapper objectMapper;

    @Transactional
    public IncidentCreated handle(VideoEventNormalized event) {
        UUID incidentId = UUID.randomUUID();
        Instant now = Instant.now();

        Incident incident = new Incident(
            incidentId,
            event.eventId(),
            "VideoEventNormalized",
            event.videoServerId(),
            event.cameraId(),
            event.channelId(),
            event.objectId(),
            event.severity().name(),
            buildTitle(event),
            buildDescription(event),
            event.occurredAt(),
            now
        );
        incidentRepository.save(incident);

        IncidentCreated created = new IncidentCreated(
            incidentId,
            event.eventId(),
            "VideoEventNormalized",
            event.videoServerId(),
            event.cameraId(),
            event.channelId(),
            event.objectId(),
            event.severity().name(),
            incident.getTitle(),
            event.occurredAt()
        );

        publish(created);
        return created;
    }

    private void publish(IncidentCreated event) {
        try {
            String payload = objectMapper.writeValueAsString(event);
            outboxPort.publish(new OutboxMessage(
                event.incidentId(),
                INCIDENT_CREATED,
                event.incidentId(),
                payload,
                event.occurredAt()
            ));
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Failed to serialize IncidentCreated", exception);
        }
    }

    private String buildTitle(VideoEventNormalized event) {
        return "Video incident: " + event.eventType();
    }

    private String buildDescription(VideoEventNormalized event) {
        return "Normalized video event received from " + event.source()
            + " with severity " + event.severity() + ".";
    }
}
