package ru.agroeco.pulse.videoserver.adapter.messaging;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import ru.agroeco.pulse.platform.application.event.EventDispatcher;
import ru.agroeco.pulse.trassir.api.event.TrassirCameraConnectionChanged;
import ru.agroeco.pulse.videoserver.adapter.persistence.CameraRepository;
import ru.agroeco.pulse.videoserver.application.IngestVideoEventUseCase;
import ru.agroeco.pulse.videoserver.application.dto.VideoEventIngestRequest;
import ru.agroeco.pulse.videoserver.domain.Camera;
import ru.agroeco.pulse.videoserver.domain.event.CameraOffline;
import ru.agroeco.pulse.videoserver.domain.event.CameraOnline;

@Slf4j
@Component
@RequiredArgsConstructor
public class TrassirCameraConnectionListener {

    private final CameraRepository cameras;
    private final IngestVideoEventUseCase ingestVideoEventUseCase; // <-- ДОБАВЛЕНО
    private final EventDispatcher events;

    @TransactionalEventListener(phase = TransactionPhase.BEFORE_COMMIT)
    public void on(TrassirCameraConnectionChanged event) {
        var cameraOpt = cameras.findByVideoServerIdAndExternalId(event.serverId(), event.cameraExternalId());

        if (cameraOpt.isEmpty()) {
            log.warn("Camera not found for connection event: serverId={}, externalId={}",
                    event.serverId(), event.cameraExternalId());
            return;
        }

        Camera camera = cameraOpt.get();
        String subtype = event.connected() ? "Connection Established" : "Connection Lost";
        String severity = event.connected() ? "INFO" : "WARNING";

        // Сохраняем в normalized_video_events для UI
        ingestVideoEventUseCase.handle(new VideoEventIngestRequest(
                event.serverId(),
                event.eventId(),
                event.eventId() + ":" + camera.getExternalId(),
                camera.getId(),
                null,
                event.occurredAt(),
                "trassir",
                subtype,
                severity
        ));

        // Создаем доменное событие для Outbox (Kafka -> Incidents)
        if (event.connected()) {
            events.dispatch(CameraOnline.builder()
                    .cameraId(camera.getId())
                    .videoServerId(event.serverId())
                    .externalId(camera.getExternalId())
                    .cameraName(camera.getName())
                    .lastSeenAt(event.occurredAt())
                    .occurredAt(event.occurredAt())
                    .build());
        } else {
            events.dispatch(CameraOffline.builder()
                    .cameraId(camera.getId())
                    .videoServerId(event.serverId())
                    .externalId(camera.getExternalId())
                    .cameraName(camera.getName())
                    .connectionStatus("Disconnected")
                    .lastSeenAt(camera.getLastSeenAt())
                    .occurredAt(event.occurredAt())
                    .build());
        }
    }
}