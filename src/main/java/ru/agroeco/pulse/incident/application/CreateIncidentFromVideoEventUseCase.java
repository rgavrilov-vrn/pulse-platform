package ru.agroeco.pulse.incident.application;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.agroeco.pulse.incident.adapter.persistence.IncidentContextRefRepository;
import ru.agroeco.pulse.incident.adapter.persistence.IncidentRepository;
import ru.agroeco.pulse.incident.adapter.persistence.ViolationCategoryRepository;
import ru.agroeco.pulse.incident.domain.*;
import ru.agroeco.pulse.incident.domain.event.IncidentCreated;
import ru.agroeco.pulse.platform.application.event.EventDispatcher;
import ru.agroeco.pulse.videoserver.api.EventSeverity;
import ru.agroeco.pulse.videoserver.api.VideoEventNormalized;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class CreateIncidentFromVideoEventUseCase {

    public static final String SOURCE_VIDEOSERVER = "videoserver";
    public static final String DOMAIN_VIDEOSERVER = "videoserver";
    public static final String DOMAIN_REF        = "ref";
    public static final String DEFAULT_CATEGORY_CODE = "EVENT_GENERAL";

    private final IncidentRepository incidentRepository;
    private final IncidentContextRefRepository contextRefRepository;
    private final ViolationCategoryRepository categories;
    private final EventDispatcher events;

    /** FIX #57: вынесено из константы; в multi-tenant можно переопределить. */
    @Value("${app.incident.system-user-id:00000000-0000-0000-0000-000000000001}")
    private UUID systemUserId;

    @Transactional
    public Optional<IncidentCreated> handle(VideoEventNormalized event) {
        UUID incidentId = UUID.randomUUID();
        Instant now = Instant.now();

        IncidentSeverity severity = mapSeverity(event.severity());
        ViolationCategoryEntity category = categories.findByCodeAndActiveTrue(DEFAULT_CATEGORY_CODE)
                .orElseThrow(() -> new IllegalStateException(
                        "Active category '" + DEFAULT_CATEGORY_CODE + "' is not configured"));

        IncidentEntity incident = IncidentEntity.builder()
                .id(incidentId)
                .source(SOURCE_VIDEOSERVER)
                .creationMode(IncidentCreationMode.INTEGRATION)
                .status(IncidentStatus.RECORDED)
                .severity(severity)
                .categoryId(category.getId())
                .eventTime(event.occurredAt())
                .receivedAt(now)
                .createdBy(systemUserId)
                .updatedBy(systemUserId)
                .sourceEventId(event.eventId())   // FIX #58
                .build();

        // FIX #58: идемпотентность на уровне БД через unique(source, source_event_id).
        try {
            incidentRepository.saveAndFlush(incident);
        } catch (DataIntegrityViolationException e) {
            if (isUniqueViolation(e, "ux_incidents_source_event_id")) {
                log.info("Duplicate video event: eventId={} → incident already exists",
                        event.eventId());
                return Optional.empty();
            }
            throw e;
        }

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
                .actorId(systemUserId)
                .occurredAt(now)
                .sourceEventId(event.eventId())
                .sourceEventType("VideoEventNormalized")
                .videoServerId(event.videoServerId())
                .cameraId(event.cameraId())
                .channelId(event.channelId())
                .objectId(event.objectId())
                .title("Video incident: " + event.eventType())
                .build();

        events.dispatch(created);
        log.info("Incident created from video event: incidentId={}, number={}, severity={}, categoryId={}",
                incidentId, incident.getNumber(), severity, category.getId());
        return Optional.of(created);
    }

    private List<IncidentContextRef> buildContextRefs(UUID incidentId, VideoEventNormalized event) {
        List<IncidentContextRef> refs = new ArrayList<>(4);
        if (event.videoServerId() != null)
            refs.add(new IncidentContextRef(incidentId, DOMAIN_VIDEOSERVER,
                    "video_server", event.videoServerId(), true, null));
        if (event.cameraId() != null)
            refs.add(new IncidentContextRef(incidentId, DOMAIN_VIDEOSERVER,
                    "camera", event.cameraId(), false, null));
        if (event.channelId() != null)
            refs.add(new IncidentContextRef(incidentId, DOMAIN_VIDEOSERVER,
                    "channel", event.channelId(), false, null));
        if (event.objectId() != null)
            refs.add(new IncidentContextRef(incidentId, DOMAIN_REF,
                    "object", event.objectId(), false, null));
        return refs;
    }

    private IncidentSeverity mapSeverity(EventSeverity sev) {
        if (sev == null) return IncidentSeverity.MEDIUM;
        return switch (sev) {
            case INFO     -> IncidentSeverity.LOW;
            case WARNING  -> IncidentSeverity.MEDIUM;
            case CRITICAL -> IncidentSeverity.CRITICAL;
        };
    }

    /** FIX #58: распознаём unique violation по SQLState + имени constraint. */
    private static boolean isUniqueViolation(Throwable t, String constraintName) {
        for (Throwable c = t; c != null; c = c.getCause()) {
            if (c instanceof java.sql.SQLException sql && "23505".equals(sql.getSQLState())) {
                String msg = sql.getMessage();
                return msg != null && msg.contains(constraintName);
            }
        }
        return false;
    }
}