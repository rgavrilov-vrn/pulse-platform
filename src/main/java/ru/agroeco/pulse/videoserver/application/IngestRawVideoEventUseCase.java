package ru.agroeco.pulse.videoserver.application;

import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.agroeco.pulse.videoserver.api.RawVideoEventRequest;
import ru.agroeco.pulse.videoserver.api.VideoEventNormalized;
import ru.agroeco.pulse.videoserver.domain.NormalizedVideoEvent;
import ru.agroeco.pulse.videoserver.domain.RawVideoEvent;
import ru.agroeco.pulse.videoserver.domain.VideoEventIngested;
import ru.agroeco.pulse.videoserver.infrastructure.persistence.NormalizedVideoEventRepository;
import ru.agroeco.pulse.videoserver.infrastructure.persistence.RawVideoEventRepository;

import java.time.Instant;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class IngestRawVideoEventUseCase {

    private static final int CURRENT_PAYLOAD_VERSION = 1;

    private final RawVideoEventRepository rawRepository;
    private final NormalizedVideoEventRepository normalizedRepository;
    private final PayloadClassifier classifier;
    private final ApplicationEventPublisher publisher;

    @Transactional
    public VideoEventNormalized handle(RawVideoEventRequest request) {
        // 1) идемпотентность: если уже приняли это событие — вернём сохранённый ответ
        return rawRepository
                .findByVideoServerIdAndExternalEventId(
                        request.videoServerId(), request.externalEventId())
                .map(this::toResponse)
                .orElseGet(() -> processNew(request));
    }

    private VideoEventNormalized processNew(RawVideoEventRequest request) {
        Instant now = Instant.now();

        RawVideoEvent raw = new RawVideoEvent(
                UUID.randomUUID(),
                request.videoServerId(),
                request.cameraId(),
                request.channelId(),
                request.externalEventId(),
                request.occurredAt(),
                request.source(),
                request.rawPayload(),
                now
        );
        rawRepository.save(raw);

        PayloadClassifier.Classification c = classifier.classify(request.rawPayload());

        NormalizedVideoEvent normalized = new NormalizedVideoEvent(
                UUID.randomUUID(),
                raw.getId(),
                request.videoServerId(),
                request.cameraId(),
                request.channelId(),
                null,
                request.occurredAt(),
                request.source(),
                c.type(),
                c.severity(),
                CURRENT_PAYLOAD_VERSION,
                now
        );
        normalizedRepository.save(normalized);

        // в audit/outbox уходит доменное событие, не API-DTO
        publisher.publishEvent(new VideoEventIngested(
                normalized.getId(),
                raw.getId(),
                request.videoServerId(),
                request.cameraId(),
                request.channelId(),
                request.occurredAt(),
                request.source(),
                c.type(),
                c.severity()
        ));

        return toResponse(normalized);
    }

    private VideoEventNormalized toResponse(RawVideoEvent raw) {
        NormalizedVideoEvent normalized = normalizedRepository.findByRawEventId(raw.getId())
                .orElseThrow(() -> new IllegalStateException(
                        "Normalized event missing for raw event " + raw.getId()));
        return toResponse(normalized);
    }

    private VideoEventNormalized toResponse(NormalizedVideoEvent n) {
        return new VideoEventNormalized(
                n.getId(),
                n.getRawEventId(),
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