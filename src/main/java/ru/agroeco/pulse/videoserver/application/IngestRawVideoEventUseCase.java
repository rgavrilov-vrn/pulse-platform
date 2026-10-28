package ru.agroeco.pulse.videoserver.application;

import java.time.Instant;
import java.util.UUID;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.agroeco.pulse.videoserver.api.RawVideoEventRequest;
import ru.agroeco.pulse.videoserver.api.VideoEventNormalized;
import ru.agroeco.pulse.videoserver.domain.NormalizedVideoEvent;
import ru.agroeco.pulse.videoserver.domain.RawVideoEvent;
import ru.agroeco.pulse.videoserver.infrastructure.persistence.NormalizedVideoEventRepository;
import ru.agroeco.pulse.videoserver.infrastructure.persistence.RawVideoEventRepository;

@Service
public class IngestRawVideoEventUseCase {

    private static final int CURRENT_PAYLOAD_VERSION = 1;

    private final RawVideoEventRepository rawVideoEventRepository;
    private final NormalizedVideoEventRepository normalizedVideoEventRepository;
    private final ApplicationEventPublisher eventPublisher;

    public IngestRawVideoEventUseCase(
        RawVideoEventRepository rawVideoEventRepository,
        NormalizedVideoEventRepository normalizedVideoEventRepository,
        ApplicationEventPublisher eventPublisher
    ) {
        this.rawVideoEventRepository = rawVideoEventRepository;
        this.normalizedVideoEventRepository = normalizedVideoEventRepository;
        this.eventPublisher = eventPublisher;
    }

    @Transactional
    public VideoEventNormalized handle(RawVideoEventRequest request) {
        UUID rawEventId = UUID.randomUUID();
        Instant now = Instant.now();

        RawVideoEvent rawEvent = new RawVideoEvent(
            rawEventId,
            request.videoServerId(),
            request.cameraId(),
            request.channelId(),
            request.occurredAt(),
            request.source(),
            request.rawPayload(),
            now
        );
        rawVideoEventRepository.save(rawEvent);

        UUID normalizedEventId = UUID.randomUUID();
        String eventType = normalizeEventType(request.rawPayload());
        String severity = normalizeSeverity(request.rawPayload());

        NormalizedVideoEvent normalizedEvent = new NormalizedVideoEvent(
            normalizedEventId,
            rawEventId,
            request.videoServerId(),
            request.cameraId(),
            request.channelId(),
            null,
            request.occurredAt(),
            request.source(),
            eventType,
            severity,
            CURRENT_PAYLOAD_VERSION,
            now
        );
        normalizedVideoEventRepository.save(normalizedEvent);

        VideoEventNormalized event = new VideoEventNormalized(
            normalizedEventId,
            rawEventId,
            request.videoServerId(),
            request.cameraId(),
            request.channelId(),
            null,
            request.occurredAt(),
            request.source(),
            eventType,
            severity,
            CURRENT_PAYLOAD_VERSION
        );

        eventPublisher.publishEvent(event);
        return event;
    }

    private String normalizeEventType(String rawPayload) {
        if (rawPayload == null || rawPayload.isBlank()) {
            return "UNKNOWN";
        }
        String payload = rawPayload.toLowerCase();
        if (payload.contains("motion")) {
            return "MOTION_DETECTED";
        }
        if (payload.contains("offline")) {
            return "CAMERA_OFFLINE";
        }
        if (payload.contains("tamper")) {
            return "CAMERA_TAMPERING";
        }
        return "VIDEO_EVENT_RECEIVED";
    }

    private String normalizeSeverity(String rawPayload) {
        if (rawPayload == null || rawPayload.isBlank()) {
            return "INFO";
        }
        String payload = rawPayload.toLowerCase();
        if (payload.contains("tamper") || payload.contains("critical")) {
            return "CRITICAL";
        }
        if (payload.contains("motion") || payload.contains("warning")) {
            return "WARNING";
        }
        return "INFO";
    }
}
