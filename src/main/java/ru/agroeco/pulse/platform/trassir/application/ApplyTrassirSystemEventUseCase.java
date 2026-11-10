package ru.agroeco.pulse.platform.trassir.application;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import ru.agroeco.pulse.platform.trassir.repository.TrassirSystemEventRepository;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

@Service
@RequiredArgsConstructor
public class ApplyTrassirSystemEventUseCase implements TrassirEventUseCase {

    private final TrassirSystemEventRepository systemEventRepository;
    private final ObjectMapper objectMapper;

    @Override
    public EventProcessingResult handle(TrassirEventContext context) {
        JsonNode data;
        try {
            data = objectMapper.readTree(context.event().data());
        } catch (Exception e) {
            return EventProcessingResult.failure("INVALID_TRASSIR_EVENT_DATA");
        }

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
        entity.setSource(context.event().source());
        entity.setData(context.event().data());
        entity.setRawEvent(context.event().data());
        systemEventRepository.save(entity);

        return EventProcessingResult.success();
    }

    private String text(JsonNode node, String field) {
        JsonNode value = node.get(field);
        return value != null && value.isString() && !value.asString().isBlank()
                ? value.asString()
                : null;
    }
}
