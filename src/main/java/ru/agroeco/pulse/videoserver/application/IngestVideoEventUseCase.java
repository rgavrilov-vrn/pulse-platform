package ru.agroeco.pulse.videoserver.application;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.agroeco.pulse.videoserver.adapter.persistence.NormalizedVideoEventRepository;
import ru.agroeco.pulse.videoserver.api.EventSeverity;
import ru.agroeco.pulse.videoserver.api.VideoEventNormalized;
import ru.agroeco.pulse.videoserver.api.VideoEventType;
import ru.agroeco.pulse.videoserver.application.dto.VideoEventIngestRequest;
import ru.agroeco.pulse.videoserver.domain.NormalizedVideoEvent;
import ru.agroeco.pulse.videoserver.domain.VideoEventIngested;

import java.sql.SQLException;
import java.time.Instant;
import java.util.UUID;

/**
 * Внутренний use case: нормализация видео-события.
 * Вызывается ТОЛЬКО из {@link ru.agroeco.pulse.videoserver.adapter.messaging.TrassirVideoEventListener}.
 * HTTP-ingest endpoint отсутствует: raw-данные живут в trassir.ingest_raw / trassir.events.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class IngestVideoEventUseCase {

    private static final int CURRENT_PAYLOAD_VERSION = 1;

    private final NormalizedVideoEventRepository repository;
    private final ApplicationEventPublisher publisher;

    @Transactional
    public VideoEventNormalized handle(VideoEventIngestRequest request) {
        // 1. Idempotency fast-path
        var existing = repository.findByVideoServerIdAndExternalEventId(
                request.videoServerId(), request.externalEventId());
        if (existing.isPresent()) {
            log.debug("Duplicate video event: serverId={}, externalEventId={}",
                    request.videoServerId(), request.externalEventId());
            return toResponse(existing.get());
        }

        VideoEventType type     = PayloadClassifier.mapSubtype(request.subtype());
        EventSeverity severity  = PayloadClassifier.mapSeverity(request.severity());

        Instant now = Instant.now();
        NormalizedVideoEvent event = new NormalizedVideoEvent(
                UUID.randomUUID(),
                request.videoServerId(),
                request.cameraId(),
                request.channelId(),
                null,                                // objectId — reserved
                request.sourceEventId(),
                request.externalEventId(),
                request.occurredAt(),
                request.source(),
                type,
                severity,
                CURRENT_PAYLOAD_VERSION,
                now,
                now
        );

        // 2. Idempotency race-safe: UNIQUE (video_server_id, external_event_id)
        try {
            repository.saveAndFlush(event);
        } catch (DataIntegrityViolationException ex) {
            if (isUniqueViolation(ex, "uq_norm_events_server_external")) {
                return repository
                        .findByVideoServerIdAndExternalEventId(
                                request.videoServerId(), request.externalEventId())
                        .map(this::toResponse)
                        .orElseThrow();
            }
            throw ex;
        }

        publisher.publishEvent(new VideoEventIngested(
                event.getId(),
                event.getSourceEventId(),
                request.videoServerId(),
                request.cameraId(),
                request.channelId(),
                request.occurredAt(),
                request.source(),
                type,
                severity
        ));
        return toResponse(event);
    }

    private static boolean isUniqueViolation(Throwable t, String constraint) {
        for (Throwable c = t; c != null; c = c.getCause()) {
            if (c instanceof SQLException sql && "23505".equals(sql.getSQLState())) {
                String msg = sql.getMessage();
                return msg != null && msg.contains(constraint);
            }
        }
        return false;
    }

    private VideoEventNormalized toResponse(NormalizedVideoEvent n) {
        return new VideoEventNormalized(
                n.getId(),
                n.getSourceEventId(),
                n.getVideoServerId(),
                n.getCameraId(),
                n.getChannelId(),
                n.getObjectId(),
                n.getOccurredAt(),
                n.getSource(),
                n.getEventType(),
                n.getSeverity(),
                n.getPayloadVersion()
        );
    }
}