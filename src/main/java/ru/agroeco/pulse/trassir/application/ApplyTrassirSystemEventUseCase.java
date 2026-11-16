package ru.agroeco.pulse.trassir.application;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import ru.agroeco.pulse.platform.api.support.CorrelationIds;
import ru.agroeco.pulse.platform.application.event.EventDispatcher;
import ru.agroeco.pulse.trassir.adapter.persistence.TrassirSystemEventRepository;
import ru.agroeco.pulse.trassir.api.event.TrassirCameraConnectionChanged;
import ru.agroeco.pulse.trassir.domain.TrassirSystemEvent;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.time.Instant;
import java.util.Locale;

@Slf4j
@Service
@RequiredArgsConstructor
public class ApplyTrassirSystemEventUseCase implements TrassirEventUseCase {

    private final TrassirSystemEventRepository systemEventRepository;
    private final EventDispatcher events;
    private final ObjectMapper objectMapper;

    @Override
    public EventProcessingResult handle(TrassirEventContext context) {
        JsonNode data = context.event().data();
        if (data == null || !data.isObject()) {
            return EventProcessingResult.failure("System event data must be an object");
        }

        String type = text(data, "type");
        if (type == null) {
            return EventProcessingResult.failure(
                    "System event data.type is required (e.g. 'Connection Lost', 'Login Successful')");
        }

        String severity = deriveSeverity(type);

        Instant occurredAt = context.event().occurredAt();
        if (occurredAt == null) occurredAt = parseTimestampRaw(data);

        JsonNode source = context.event().source();
        String objectId   = text(source, "object_id");
        String objectName = text(source, "object_name");

        TrassirSystemEvent entity = new TrassirSystemEvent();
        entity.setServerId(context.serverId());
        entity.setEventId(context.event().eventId());
        entity.setEventType(type);
        entity.setEventVersion(context.event().eventVersion());
        entity.setOccurredAt(occurredAt);
        entity.setObservedAt(context.event().observedAt());
        entity.setSeverity(severity);
        entity.setSource(toJson(source));
        entity.setData(toJson(data));
        entity.setRawEvent(toJson(context.event()));
        entity.setObjectId(objectId);
        entity.setObjectName(objectName);
        entity.setP1(text(data, "p1"));
        entity.setP2(text(data, "p2"));
        entity.setP3(text(data, "p3"));

        systemEventRepository.save(entity);

        // МОСТИК: отправляем интеграционное событие для videoserver
        bridgeConnectionEvent(context, type, objectId, objectName, occurredAt);

        log.debug("Stored trassir.system_event: eventId={}, type={}, severity={}, object={}",
                context.event().eventId(), type, severity, objectName);
        return EventProcessingResult.success();
    }

    private void bridgeConnectionEvent(TrassirEventContext context, String type,
                                       String objectId, String objectName, Instant occurredAt) {
        if (objectId == null || objectId.isBlank()) {
            return;
        }
        boolean isLost = type.contains("Connection Lost");
        boolean isEstablished = type.contains("Connection Established");

        if (isLost || isEstablished) {
            events.dispatch(new TrassirCameraConnectionChanged(
                    context.event().eventId(),
                    1,
                    context.serverId(),
                    objectId,
                    objectName,
                    isEstablished,
                    occurredAt != null ? occurredAt : context.event().observedAt(),
                    CorrelationIds.current()
            ));
        }
    }

    private static String deriveSeverity(String type) {
        if (type == null) return "INFO";
        String lower = type.toLowerCase(Locale.ROOT);
        if (lower.contains("critical") || lower.contains("alert") || lower.contains("emergency")) return "CRITICAL";
        if (lower.contains("lost") || lower.contains("fail") || lower.contains("error")
                || lower.contains("tamper") || lower.contains("disconnect")
                || lower.contains("denied") || lower.contains("unavailable")) return "WARNING";
        return "INFO";
    }

    private static Instant parseTimestampRaw(JsonNode data) {
        JsonNode ts = data.get("timestamp_raw");
        if (ts == null || ts.isNull()) return null;
        try {
            long micros = ts.isString() ? Long.parseLong(ts.asString().trim()) : ts.asLong();
            if (micros <= 0) return null;
            return Instant.ofEpochSecond(micros / 1_000_000L, (micros % 1_000_000L) * 1_000L);
        } catch (Exception e) { return null; }
    }

    private String text(JsonNode node, String field) {
        if (node == null || node.isNull()) return null;
        JsonNode v = node.get(field);
        return v != null && v.isString() && !v.asString().isBlank() ? v.asString() : null;
    }

    private String toJson(Object node) {
        if (node == null) return "{}";
        try { return objectMapper.writeValueAsString(node); }
        catch (Exception e) { log.warn("Cannot serialize TRASSIR system-event payload", e); return "{}"; }
    }
}