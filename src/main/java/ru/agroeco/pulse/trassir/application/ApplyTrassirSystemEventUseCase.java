package ru.agroeco.pulse.trassir.application;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import ru.agroeco.pulse.trassir.adapter.persistence.TrassirSystemEventRepository;
import ru.agroeco.pulse.trassir.domain.TrassirSystemEvent;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.time.Instant;
import java.util.Locale;

/**
 * Обработка {@code trassir.system_event} (event_version 1.1).
 *
 * <p>Реальный контракт payload:</p>
 * <pre>
 * "data": {
 *   "type": "Login Successful, %1 from %2" | "Connection Established" | "Connection Lost" | ...,
 *   "p1": "Admin", "p2": "10.81.0.183", "p3": "",
 *   "timestamp_raw": "1794568488723506"   // microsecond epoch
 * }
 * </pre>
 *
 * <p><b>ВАЖНО:</b> поля {@code data.severity} в контракте <b>нет</b> — оно выводится из
 * {@code data.type}. Ранее handler требовал {@code severity} и отклонял все события
 * с {@code "data.severity must be a nonblank string"} → 100% system_events терялись.</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ApplyTrassirSystemEventUseCase implements TrassirEventUseCase {

    private final TrassirSystemEventRepository systemEventRepository;
    private final ObjectMapper objectMapper;

    @Override
    public EventProcessingResult handle(TrassirEventContext context) {
        JsonNode data = context.event().data();
        if (data == null || !data.isObject()) {
            return EventProcessingResult.failure("System event data must be an object");
        }

        // data.type — фактический тип события (обязателен)
        String type = text(data, "type");
        if (type == null) {
            return EventProcessingResult.failure(
                    "System event data.type is required (e.g. 'Connection Lost', 'Login Successful')");
        }

        // severity выводим из type (TRASSIR не присылает отдельно)
        String severity = deriveSeverity(type);

        // occurred_at: если в envelope null — берём из data.timestamp_raw (microsecond epoch)
        Instant occurredAt = context.event().occurredAt();
        if (occurredAt == null) {
            occurredAt = parseTimestampRaw(data);
        }

        TrassirSystemEvent entity = new TrassirSystemEvent();
        entity.setServerId(context.serverId());
        entity.setEventId(context.event().eventId());
        // event_type — фактический тип события, не "trassir.system_event".
        // Позволяет фильтровать "все Connection Lost за час".
        entity.setEventType(type);
        entity.setEventVersion(context.event().eventVersion());
        entity.setOccurredAt(occurredAt);
        entity.setObservedAt(context.event().observedAt());
        entity.setSeverity(severity);
        entity.setSource(toJson(context.event().source()));
        entity.setData(toJson(data));
        // raw_event — полный envelope-event (включая source и data)
        entity.setRawEvent(toJson(context.event()));

        systemEventRepository.save(entity);

        log.debug("Stored trassir.system_event: eventId={}, type={}, severity={}, object={}",
                context.event().eventId(), type, severity,
                text(context.event().source(), "object_name"));
        return EventProcessingResult.success();
    }

    /**
     * Классификация severity по типу события. Примеры:
     * <ul>
     *   <li>"Connection Lost" / "Failed" / "Error" / "Tamper" → WARNING</li>
     *   <li>"Critical" / "Alert" → CRITICAL</li>
     *   <li>"Connection Established" / "Login Successful" → INFO</li>
     * </ul>
     */
    private static String deriveSeverity(String type) {
        if (type == null) return "INFO";
        String lower = type.toLowerCase(Locale.ROOT);
        if (lower.contains("critical") || lower.contains("alert") || lower.contains("emergency")) {
            return "CRITICAL";
        }
        if (lower.contains("lost")
                || lower.contains("fail")
                || lower.contains("error")
                || lower.contains("tamper")
                || lower.contains("disconnect")
                || lower.contains("denied")
                || lower.contains("unavailable")) {
            return "WARNING";
        }
        return "INFO";
    }

    /**
     * data.timestamp_raw — микросекунды с epoch (в payload может быть строкой или числом).
     * Возвращает null, если поле отсутствует/битое.
     */
    private static Instant parseTimestampRaw(JsonNode data) {
        JsonNode ts = data.get("timestamp_raw");
        if (ts == null || ts.isNull()) return null;
        try {
            long micros = ts.isString() ? Long.parseLong(ts.asString().trim()) : ts.asLong();
            if (micros <= 0) return null;
            return Instant.ofEpochSecond(micros / 1_000_000L, (micros % 1_000_000L) * 1_000L);
        } catch (Exception e) {
            return null;
        }
    }

    private String text(JsonNode node, String field) {
        if (node == null || node.isNull()) return null;
        JsonNode v = node.get(field);
        return v != null && v.isString() && !v.asString().isBlank() ? v.asString() : null;
    }

    private String toJson(Object node) {
        if (node == null) return "{}";
        try {
            return objectMapper.writeValueAsString(node);
        } catch (Exception e) {
            log.warn("Cannot serialize TRASSIR system-event payload", e);
            return "{}";
        }
    }
}