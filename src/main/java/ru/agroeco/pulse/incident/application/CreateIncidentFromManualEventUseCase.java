package ru.agroeco.pulse.incident.application;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import ru.agroeco.pulse.incident.adapter.persistence.IncidentContextRefRepository;
import ru.agroeco.pulse.incident.adapter.persistence.IncidentRepository;
import ru.agroeco.pulse.incident.adapter.persistence.ViolationCategoryRepository;
import ru.agroeco.pulse.incident.domain.*;
import ru.agroeco.pulse.incident.domain.event.IncidentCreated;
import ru.agroeco.pulse.platform.application.event.EventDispatcher;
import ru.agroeco.pulse.trassir.api.event.TrassirManualIncidentRequested;
import ru.agroeco.pulse.trassir.application.TrassirScreenshotAttachmentService;
import ru.agroeco.pulse.videoserver.api.CameraLookupService;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class CreateIncidentFromManualEventUseCase {

    private static final UUID SYSTEM_USER = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private static final String DEFAULT_CATEGORY_CODE = "EVENT_GENERAL";

    private final CameraLookupService cameraLookup;
    private final ViolationCategoryRepository categories;
    private final IncidentRepository incidents;
    private final IncidentContextRefRepository contextRefs;
    private final EventDispatcher events;
    private final TrassirScreenshotAttachmentService screenshotAttachments;

    @Transactional
    public void handle(TrassirManualIncidentRequested event) {
        var objOpt = cameraLookup.findByExternalId(event.objectId());
        if (objOpt.isEmpty()) {
            log.warn("Object not found for manual incident: {}", event.objectId());
            return;
        }

        var obj = objOpt.get();
        var category = categories.findByCodeAndActiveTrue(DEFAULT_CATEGORY_CODE)
                .orElseThrow(() -> new IllegalStateException("Category EVENT_GENERAL not configured"));

        Map<String, Object> metadata = new HashMap<>();
        if (event.imageFile() != null) metadata.put("image_file", event.imageFile());
        if (event.videoTimestamp() != null) metadata.put("video_timestamp", event.videoTimestamp());
        if (event.reason() != null) metadata.put("reason", event.reason());

        var incident = IncidentEntity.builder()
                .id(UUID.randomUUID())
                .source("manual")
                .creationMode(IncidentCreationMode.MANUAL)
                .status(IncidentStatus.DRAFT)
                .severity(IncidentSeverity.MEDIUM)
                .categoryId(category.getId())
                .eventTime(event.occurredAt())
                .receivedAt(Instant.now())
                .createdBy(SYSTEM_USER)
                .updatedBy(SYSTEM_USER)
                .comment(event.comment())
                .metadata(metadata)
                .build();

        incidents.saveAndFlush(incident);

        // Привязываем канал или камеру
        String entityType = obj.channelId() != null ? "channel" : "camera";
        UUID entityId = obj.channelId() != null ? obj.channelId() : obj.cameraId();

        contextRefs.save(new IncidentContextRef(
                incident.getId(), "videoserver", entityType, entityId, true, obj.name()
        ));

        attachScreenshotAfterCommit(
                incident.getId(),
                event.objectId(),
                event.videoTimestamp(),
                event.imageFile()
        );

        events.dispatch(IncidentCreated.builder()
                .incidentId(incident.getId())
                .number(incident.getNumber())
                .source("manual")
                .status(incident.getStatus())
                .severity(incident.getSeverity())
                .eventTime(incident.getEventTime())
                .actorId(SYSTEM_USER)
                .occurredAt(Instant.now())
                .build());

        log.info("Created MANUAL DRAFT incident: id={}, number={}, object={}",
                incident.getId(), incident.getNumber(), obj.name());
    }

    private void attachScreenshotAfterCommit(
            UUID incidentId,
            String objectId,
            String videoTimestamp,
            String imageFile
    ) {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    screenshotAttachments.attachScreenshot(
                            incidentId,
                            objectId,
                            videoTimestamp,
                            imageFile
                    );
                }
            });
        } else {
            screenshotAttachments.attachScreenshot(
                    incidentId,
                    objectId,
                    videoTimestamp,
                    imageFile
            );
        }
    }
}
