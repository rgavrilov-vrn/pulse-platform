package ru.agroeco.pulse.videoserver.application;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.agroeco.pulse.outbox.api.OutboxMessage;
import ru.agroeco.pulse.outbox.api.OutboxPort;
import ru.agroeco.pulse.videoserver.api.RawVideoEventRequest;
import ru.agroeco.pulse.videoserver.api.VideoEventNormalized;
import ru.agroeco.pulse.videoserver.domain.NormalizedVideoEvent;
import ru.agroeco.pulse.videoserver.domain.RawVideoEvent;
import ru.agroeco.pulse.videoserver.infrastructure.persistence.NormalizedVideoEventRepository;
import ru.agroeco.pulse.videoserver.infrastructure.persistence.RawVideoEventRepository;

@Service
public class IngestRawVideoEventUseCase {

    public static final String VIDEO_EVENT_NORMALIZED = "VideoEventNormalized";
    private static final int CURRENT_PAYLOAD_VERSION = 1;

    private final RawVideoEventRepository rawVideoEventRepository;
    private final NormalizedVideoEventRepository normalizedVideoEventRepository;
    private final OutboxPort outboxPort;
    private final ObjectMapper objectMapper;

    public IngestRawVideoEventUseCase(
        RawVideoEventRepository rawVideoEventRepository,
        NormalizedVideoEventRepository normalizedVideoEventRepository,
        OutboxPort outboxPort,
        ObjectMapper objectMapper
    ) {
        this.rawVideoEventRepository = rawVideoEventRepository;
        this.normalizedVideoEventRepository = normalizedVideoEventRepository;
        this.outboxPort = outboxPort;
        this.objectMapper = objectMapper;
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

        publishToOutbox(event);
        return event;
    }

    private void publishToOutbox(VideoEventNormalized event) {
        try {
            String payload = objectMapper.writeValueAsString(event);
            outboxPort.publish(new OutboxMessage(
                event.eventId(),
                VIDEO_EVENT_NORMALIZED,
                event.eventId(),
                payload,
                event.occurredAt()
            ));
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Failed to serialize VideoEventNormalized", exception);
        }
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
