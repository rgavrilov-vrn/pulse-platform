package ru.agroeco.pulse.incident.application;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.agroeco.pulse.incident.adapter.persistence.IncidentContextRefRepository;
import ru.agroeco.pulse.incident.adapter.persistence.IncidentRepository;
import ru.agroeco.pulse.incident.domain.IncidentContextRef;
import ru.agroeco.pulse.incident.domain.IncidentCreationMode;
import ru.agroeco.pulse.incident.domain.IncidentEntity;
import ru.agroeco.pulse.incident.domain.IncidentSeverity;
import ru.agroeco.pulse.incident.domain.IncidentStatus;
import ru.agroeco.pulse.incident.domain.event.IncidentCreated;
import ru.agroeco.pulse.platform.application.event.EventDispatcher;
import ru.agroeco.pulse.videoserver.api.EventSeverity;
import ru.agroeco.pulse.videoserver.api.VideoEventNormalized;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class CreateIncidentFromVideoEventUseCase {

    public static final String SOURCE_VIDEOSERVER = "videoserver";
    public static final String DOMAIN_VIDEOSERVER = "videoserver";
    public static final String DOMAIN_REF = "ref";

    /**
     * Well-known SYSTEM user id — синхронизирован с 003_seed_system_user.sql.
     */
    public static final UUID SYSTEM_USER_ID =
            UUID.fromString("00000000-0000-0000-0000-000000000001");

    private final IncidentRepository incidentRepository;
    private final IncidentContextRefRepository contextRefRepository;
    private final EventDispatcher events;

    @Transactional
    public IncidentCreated handle(VideoEventNormalized event) {
        UUID incidentId = UUID.randomUUID();
        Instant now = Instant.now();

        IncidentSeverity severity = mapSeverity(event.severity());
        String title = "Video incident: " + event.eventType();

        IncidentEntity incident = IncidentEntity.builder()
                .id(incidentId)
                .source(SOURCE_VIDEOSERVER)
                .creationMode(IncidentCreationMode.INTEGRATION)
                .status(IncidentStatus.RECORDED)
                .severity(severity)
                .eventTime(event.occurredAt())
                .receivedAt(now)
                .createdBy(SYSTEM_USER_ID)
                .updatedBy(SYSTEM_USER_ID)
                .build();

        // saveAndFlush — чтобы @Generated(number) заполнился до публикации события
        incidentRepository.saveAndFlush(incident);

        List<IncidentContextRef> refs = buildContextRefs(incidentId, event);
        if (!refs.isEmpty()) {
            contextRefRepository.saveAll(refs);
        }

        IncidentCreated created = IncidentCreated.builder()
                .incidentId(incidentId)
                .number(incident.getNumber())
                .source(SOURCE_VIDEOSERVER)
                .status(incident.getStatus())
                .severity(incident.getSeverity())
                .eventTime(event.occurredAt())
                .actorId(SYSTEM_USER_ID)
                .occurredAt(now)
                // интеграционный контекст
                .sourceEventId(event.eventId())
                .sourceEventType("VideoEventNormalized")
                .videoServerId(event.videoServerId())
                .cameraId(event.cameraId())
                .channelId(event.channelId())
                .objectId(event.objectId())
                .title(title)
                .build();

        // Единая точка публикации:
        //   AuditEventListener   (BEFORE_COMMIT) → audit_log
        //   OutboxListener       (BEFORE_COMMIT) → outbox_events → Kafka
        // Никакого прямого OutboxPort в incident-модуле больше нет.
        events.dispatch(created);

        log.info("Incident created from video event: incidentId={}, number={}, severity={}",
                incidentId, incident.getNumber(), severity);
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
}