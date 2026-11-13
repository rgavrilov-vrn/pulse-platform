package ru.agroeco.pulse.videoserver.adapter.messaging;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import ru.agroeco.pulse.trassir.api.event.TrassirVideoEventReceived;
import ru.agroeco.pulse.videoserver.adapter.persistence.CameraRepository;
import ru.agroeco.pulse.videoserver.adapter.persistence.VideoServerRepository;
import ru.agroeco.pulse.videoserver.application.IngestVideoEventUseCase;
import ru.agroeco.pulse.videoserver.application.dto.VideoEventIngestRequest;
import ru.agroeco.pulse.videoserver.domain.Camera;
import ru.agroeco.pulse.videoserver.domain.VideoServer;

import java.util.Optional;

@Slf4j
@Component
@RequiredArgsConstructor
public class TrassirVideoEventListener {

    private final VideoServerRepository servers;
    private final CameraRepository cameras;
    private final IngestVideoEventUseCase ingest;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void on(TrassirVideoEventReceived event) {
        try {
            Optional<VideoServer> serverOpt = servers.findById(event.serverId());
            if (serverOpt.isEmpty()) {
                log.warn("Video event for unknown serverId={}, skipping", event.serverId());
                return;
            }
            VideoServer server = serverOpt.get();
            if (!"trassir".equals(server.getSource())) {
                log.warn("Video event for serverId={} with source='{}', skipping",
                        event.serverId(), server.getSource());
                return;
            }

            Optional<Camera> cameraOpt = cameras.findByVideoServerIdAndExternalId(
                    event.serverId(), event.objectKey());
            if (cameraOpt.isEmpty()) {
                log.warn("Video event for unknown objectKey={} on serverId={}, skipping "
                                + "(will be created on next snapshot)",
                        event.objectKey(), event.serverId());
                return;
            }

            ingest.handle(new VideoEventIngestRequest(
                    event.serverId(),
                    event.eventId(),                              // source_event_id
                    event.eventId().toString(),                   // external_event_id
                    cameraOpt.get().getId(),
                    null,                                          // channelId — future
                    event.occurredAt(),
                    "trassir",
                    event.videoEventType(),
                    event.severity()
            ));
        } catch (RuntimeException e) {
            log.error("Video event ingest failed: eventId={}, serverId={}",
                    event.eventId(), event.serverId(), e);
            // Не пробрасываем: транзакция trassir уже закоммичена; потеря события — допустима
            // (в будущем: DLT / retry через outbox).
        }
    }
}