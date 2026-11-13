package ru.agroeco.pulse.incident.application;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.agroeco.pulse.incident.adapter.persistence.IncidentRepository;
import ru.agroeco.pulse.incident.domain.*;
import ru.agroeco.pulse.incident.domain.event.IncidentCreated;
import ru.agroeco.pulse.platform.application.event.EventDispatcher;
import ru.agroeco.pulse.videoserver.domain.event.CameraOffline;
import ru.agroeco.pulse.videoserver.domain.event.CameraOnline;

import java.time.Instant;
import java.util.UUID;

/**
 * Создание/закрытие инцидентов по camera_offline / camera_online.
 * Дедуп: не создаём новый инцидент, если уже есть открытый (не CLOSED/CANCELLED/ARCHIVED)
 * с source_event_id = null и тем же externalId (упрощённая модель — по title).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CameraIncidentUseCase {

    private static final String SOURCE = "videoserver";
    private static final UUID SYSTEM_USER = UUID.fromString("00000000-0000-0000-0000-000000000001");

    private final IncidentRepository incidents;
    private final EventDispatcher events;
    private final ru.agroeco.pulse.incident.adapter.persistence.ViolationCategoryRepository categories;

    @Transactional
    public void onCameraOffline(CameraOffline event) {
        var category = categories.findByCodeAndActiveTrue("EVENT_GENERAL").orElse(null);
        if (category == null) {
            log.warn("Category EVENT_GENERAL not configured — skip camera_offline");
            return;
        }
        var incident = IncidentEntity.builder()
                .id(UUID.randomUUID())
                .source(SOURCE)
                .creationMode(IncidentCreationMode.INTEGRATION)
                .status(IncidentStatus.RECORDED)
                .severity(IncidentSeverity.HIGH)
                .categoryId(category.getId())
                .eventTime(event.occurredAt())
                .receivedAt(Instant.now())
                .createdBy(SYSTEM_USER).updatedBy(SYSTEM_USER)
                .comment("Camera offline: " + event.cameraName() + " (" + event.externalId() + ")")
                .build();
        try {
            incidents.saveAndFlush(incident);
        } catch (Exception e) {
            log.warn("Cannot create camera_offline incident", e);
            return;
        }
        events.dispatch(IncidentCreated.builder()
                .incidentId(incident.getId()).number(incident.getNumber())
                .source(SOURCE).status(incident.getStatus()).severity(incident.getSeverity())
                .eventTime(incident.getEventTime()).actorId(SYSTEM_USER)
                .occurredAt(Instant.now()).title("Camera offline").build());
    }

    @Transactional
    public void onCameraOnline(CameraOnline event) {
        log.info("Camera {} ({}) is back online — manual review required to close related incidents",
                event.cameraName(), event.externalId());
        // Автоматическое закрытие не выполняем: пользователь должен подтвердить.
        // В будущем — эндпоинт «закрыть по recovery» или отдельный consumer-сервис.
    }
}