package ru.agroeco.pulse.platform.trassir.application;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import ru.agroeco.pulse.trassir.application.EventProcessingResult;
import ru.agroeco.pulse.trassir.application.TrassirEventContext;
import ru.agroeco.pulse.trassir.application.TrassirEventUseCase;
import ru.agroeco.pulse.trassir.domain.TrassirSystemEvent;
import ru.agroeco.pulse.trassir.repository.TrassirSystemEventRepository;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

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
                    "System event data.severity must be a nonblank string"
            );
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
        entity.setRawEvent(toJson(context.event().data()));
        systemEventRepository.save(entity);

        return EventProcessingResult.success();
    }

    private String toJson(JsonNode node) {
        if (node == null || node.isMissingNode() || node.isNull()) {
            return "{}";
        }

        try {
            return objectMapper.writeValueAsString(node);
        } catch (Exception e) {
            return "{}";
        }
    }

    private String text(JsonNode node, String field) {
        JsonNode value = node.get(field);
        return value != null && value.isString() && !value.asString().isBlank()
                ? value.asString()
                : null;
    }
}
