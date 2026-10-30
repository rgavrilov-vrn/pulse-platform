package ru.agroeco.pulse.incident.application;

import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.agroeco.pulse.incident.adapter.persistence.IncidentRepository;
import ru.agroeco.pulse.incident.api.IncidentCreated;
import ru.agroeco.pulse.incident.domain.*;
import ru.agroeco.pulse.incident.adapter.persistence.IncidentContextRefRepository;
import ru.agroeco.pulse.outbox.api.OutboxMessage;
import ru.agroeco.pulse.outbox.api.OutboxPort;
import ru.agroeco.pulse.videoserver.api.EventSeverity;
import ru.agroeco.pulse.videoserver.api.VideoEventNormalized;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CreateIncidentFromVideoEventUseCase {

    public static final String INCIDENT_CREATED = "IncidentCreated";
    public static final String SOURCE_VIDEOSERVER = "videoserver";
    public static final String DOMAIN_VIDEOSERVER = "videoserver";
    public static final String DOMAIN_REF = "ref";

    /**
     * Well-known SYSTEM user id — синхронизирован с 003_seed_system_user.sql.
     * Используется как created_by для интеграционных инцидентов.
     */
    public static final UUID SYSTEM_USER_ID =
            UUID.fromString("00000000-0000-0000-0000-000000000001");

    private final IncidentRepository incidentRepository;
    private final IncidentContextRefRepository contextRefRepository;
    private final OutboxPort outboxPort;
    private final ObjectMapper objectMapper;

    @Transactional
    public IncidentCreated handle(VideoEventNormalized event) {
        UUID incidentId = UUID.randomUUID();
        Instant now = Instant.now();

        IncidentEntity incident = IncidentEntity.builder()
                .id(incidentId)                 // или null — @PrePersist проставит
                .source(SOURCE_VIDEOSERVER)
                .creationMode(IncidentCreationMode.INTEGRATION)
                .status(IncidentStatus.RECORDED)
                .severity(IncidentSeverity.MEDIUM)
                .eventTime(event.occurredAt())
                .receivedAt(now)
                .createdBy(SYSTEM_USER_ID)
                .updatedBy(SYSTEM_USER_ID)
                .build();

        // saveAndFlush — чтобы @Generated(number) заполнился до возврата
        incidentRepository.saveAndFlush(incident);

        List<IncidentContextRef> refs = buildContextRefs(incidentId, event);
        if (!refs.isEmpty()) {
            contextRefRepository.saveAll(refs);
        }

        IncidentCreated created = new IncidentCreated(
                incidentId,
                event.eventId(),
                "VideoEventNormalized",
                event.videoServerId(),
                event.cameraId(),
                event.channelId(),
                event.objectId(),
                incident.getSeverity().name(),
                "Video incident: " + event.eventType(),
                event.occurredAt()
        );

        publish(created);
        return created;
    }

    private List<IncidentContextRef> buildContextRefs(UUID incidentId, VideoEventNormalized event) {
        List<IncidentContextRef> refs = new ArrayList<>(4);
        if (event.videoServerId() != null) {
            refs.add(new IncidentContextRef(incidentId, DOMAIN_VIDEOSERVER,
                    "video_server", event.videoServerId(), true, null));
        }
        if (event.cameraId() != null) {
            refs.add(new IncidentContextRef(incidentId, DOMAIN_VIDEOSERVER,
                    "camera", event.cameraId(), false, null));
        }
        if (event.channelId() != null) {
            refs.add(new IncidentContextRef(incidentId, DOMAIN_VIDEOSERVER,
                    "channel", event.channelId(), false, null));
        }
        if (event.objectId() != null) {
            // было "refs" (модуль), должно быть "ref" (схема) — см. 006
            refs.add(new IncidentContextRef(incidentId, DOMAIN_REF,
                    "object", event.objectId(), false, null));
        }
        return refs;
    }

    private IncidentSeverity mapSeverity(EventSeverity sev) {
        if (sev == null) return IncidentSeverity.MEDIUM;
        return switch (sev) {
            case INFO -> IncidentSeverity.LOW;
            case WARNING -> IncidentSeverity.MEDIUM;
            case CRITICAL -> IncidentSeverity.CRITICAL;
        };
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
        } catch (JacksonException exception) {
            throw new IllegalStateException("Failed to serialize IncidentCreated", exception);
        }
    }
}