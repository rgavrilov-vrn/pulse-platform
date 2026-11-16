package ru.agroeco.pulse.trassir.application;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import ru.agroeco.pulse.platform.application.event.EventDispatcher;
import ru.agroeco.pulse.trassir.adapter.persistence.TrassirManualIncidentRawRepository;
import ru.agroeco.pulse.trassir.api.event.TrassirManualIncidentRequested;
import ru.agroeco.pulse.trassir.domain.TrassirManualIncidentRaw;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class ApplyTrassirManualIncidentUseCase implements TrassirEventUseCase {

    private final TrassirManualIncidentRawRepository rawRepository;
    private final TrassirAdminAuditService auditService;
    private final EventDispatcher events;
    private final ObjectMapper objectMapper;

    @Override
    public EventProcessingResult handle(TrassirEventContext context) {
        JsonNode data = context.event().data();
        JsonNode source = context.event().source();

        if (data == null || !data.isObject() || source == null || !source.isObject()) {
            return EventProcessingResult.failure("Manual incident source and data must be objects");
        }

        String objectId = text(source, "object_id");
        if (objectId == null) {
            return EventProcessingResult.failure("Manual incident source.object_id is required");
        }

        // Защита от дубликатов при Retry
        if (rawRepository.existsByEventId(context.event().eventId())) {
            log.info("Manual incident raw already exists, skipping duplicate: eventId={}", context.event().eventId());
            return EventProcessingResult.success();
        }

        String objectName = text(source, "object_name");
        String objectClass = text(source, "object_class");
        String comment = text(data, "comment");
        String reason = text(data, "reason");
        String videoTimestamp = text(data, "video_timestamp");
        String imageFile = text(data, "image_file");

        Instant occurredAt = context.event().occurredAt() != null
                ? context.event().occurredAt()
                : context.event().observedAt();

        // 1. Сохраняем в специализированный raw
        TrassirManualIncidentRaw raw = new TrassirManualIncidentRaw();
        raw.setId(UUID.randomUUID());
        raw.setEventId(context.event().eventId());
        raw.setOccurredAt(occurredAt);
        raw.setObservedAt(context.event().observedAt());
        raw.setObjectId(objectId);
        raw.setObjectName(objectName);
        raw.setObjectClass(objectClass);
        raw.setComment(comment);
        raw.setReason(reason);
        raw.setVideoTimestamp(videoTimestamp);
        raw.setImageFile(imageFile);
        raw.setRawPayload(toJson(context.event()));
        raw.setCreatedAt(Instant.now());
        rawRepository.save(raw);

        // 2. Пишем в Audit Log
        auditService.recordSuccess(
                "MANUAL_INCIDENT_RECEIVED",
                "trassir.manual_incidents_raw",
                Map.of(
                        "eventId", context.event().eventId().toString(),
                        "objectId", objectId,
                        "imageFile", imageFile != null ? imageFile : ""
                ),
                1
        );

        // 3. Генерируем интеграционное событие (улетит в videoserver и incident)
        events.dispatch(new TrassirManualIncidentRequested(
                context.event().eventId(),
                1,
                objectId,
                objectName,
                objectClass,
                comment,
                reason,
                videoTimestamp,
                imageFile,
                null,
                occurredAt
        ));

        log.info("Manual incident processed: eventId={}, objectId={}", context.event().eventId(), objectId);
        return EventProcessingResult.success();
    }

    private String text(JsonNode node, String field) {
        if (node == null || node.isNull()) return null;
        JsonNode v = node.get(field);
        return v != null && v.isString() && !v.asString().isBlank() ? v.asString() : null;
    }

    private String toJson(Object obj) {
        try { return objectMapper.writeValueAsString(obj); }
        catch (Exception e) { return "{}"; }
    }
}
