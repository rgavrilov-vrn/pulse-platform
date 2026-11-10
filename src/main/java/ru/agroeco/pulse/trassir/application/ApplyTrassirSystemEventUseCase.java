package ru.agroeco.pulse.trassir.application;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import ru.agroeco.pulse.trassir.domain.TrassirSystemEvent;
import ru.agroeco.pulse.trassir.repository.TrassirSystemEventRepository;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

@Slf4j
@Service
@RequiredArgsConstructor
public class ApplyTrassirSystemEventUseCase implements TrassirEventUseCase {

    private final TrassirSystemEventRepository systemEventRepository;
    private final ObjectMapper objectMapper;

    @Override
    public EventProcessingResult handle(TrassirEventContext context) {
        JsonNode data = context.event().data();

        String severity = text(data, "severity");
        if (severity == null) {
            return EventProcessingResult.failure(
                    "System event data.severity must be a nonblank string");
        }

        TrassirSystemEvent entity = new TrassirSystemEvent();
        entity.setServerId(context.serverId());
        entity.setEventId(context.event().eventId());
        entity.setEventType(context.event().eventType());
        entity.setEventVersion(context.event().eventVersion());
        entity.setOccurredAt(context.event().occurredAt());
        entity.setObservedAt(context.event().observedAt());
        entity.setSeverity(severity);
        entity.setSource(toJson(context.event().source()));
        entity.setData(toJson(context.event().data()));
        // FIX #83 (в партии 3 остался bug только здесь): raw_event = полный event,
        // а не только data. Восстановление исходного события из БД становится возможным.
        entity.setRawEvent(toJson(context.event()));
        systemEventRepository.save(entity);

        return EventProcessingResult.success();
    }

    private String toJson(Object node) {
        if (node == null) return "{}";
        try {
            return objectMapper.writeValueAsString(node);
        } catch (Exception e) {
            // FIX #144: раньше — молча "{}"; теперь видно в логах.
            log.warn("Cannot serialize TRASSIR system-event payload", e);
            return "{}";
        }
    }

    private String text(JsonNode node, String field) {
        JsonNode value = node.get(field);
        return value != null && value.isString() && !value.asString().isBlank()
                ? value.asString() : null;
    }
}