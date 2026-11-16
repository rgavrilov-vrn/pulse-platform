package ru.agroeco.pulse.videoserver.adapter.messaging;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import ru.agroeco.pulse.trassir.api.event.TrassirManualIncidentRequested;
import ru.agroeco.pulse.videoserver.api.CameraLookupService;
import ru.agroeco.pulse.videoserver.application.IngestVideoEventUseCase;
import ru.agroeco.pulse.videoserver.application.dto.VideoEventIngestRequest;

@Slf4j
@Component
@RequiredArgsConstructor
public class TrassirManualIncidentVideoserverListener {

    private final CameraLookupService lookupService;
    private final IngestVideoEventUseCase ingestVideoEventUseCase;

    @TransactionalEventListener(phase = TransactionPhase.BEFORE_COMMIT)
    public void on(TrassirManualIncidentRequested event) {
        var objOpt = lookupService.findByExternalId(event.objectId());
        if (objOpt.isEmpty()) {
            log.warn("Camera/Channel not found for manual incident: objectId={}", event.objectId());
            return;
        }

        var obj = objOpt.get();

        ingestVideoEventUseCase.handle(new VideoEventIngestRequest(
                obj.videoServerId(),
                event.eventId(),
                event.eventId().toString(),
                obj.cameraId(),
                obj.channelId(), // Передаем ID канала (если есть)
                event.occurredAt(),
                "trassir",
                "MANUAL_INCIDENT",
                "INFO"
        ));
    }
}