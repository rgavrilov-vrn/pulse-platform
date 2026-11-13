package ru.agroeco.pulse.trassir.application;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import ru.agroeco.pulse.platform.api.support.CorrelationIds;
import ru.agroeco.pulse.platform.application.event.EventDispatcher;
import ru.agroeco.pulse.trassir.api.event.TrassirVideoEventReceived;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

@Slf4j
@Service
@RequiredArgsConstructor
public class ApplyTrassirVideoEventUseCase implements TrassirEventUseCase {

    private final EventDispatcher events;
    private final ObjectMapper objectMapper;

    @Override
    public EventProcessingResult handle(TrassirEventContext context) {
        JsonNode data = context.event().data();
        if (data == null || !data.isObject()) {
            return EventProcessingResult.failure("video_event data must be an object");
        }

        String objectKey = text(data, "object_id");
        if (objectKey == null) {
            return EventProcessingResult.failure("video_event data.object_id is required");
        }
        String subtype = text(data, "subtype");
        if (subtype == null) {
            // fallback на "type" — некоторые агенты используют "type"
            subtype = text(data, "type");
        }
        if (subtype == null) {
            return EventProcessingResult.failure("video_event data.subtype is required");
        }
        String severity = text(data, "severity");
        if (severity == null) {
            severity = "INFO";  // дефолт
        }
        String channelExternalId = text(data, "channel_id");

        events.dispatch(new TrassirVideoEventReceived(
                context.event().eventId(),
                1,
                context.serverId(),
                objectKey,
                channelExternalId,
                subtype,
                severity,
                context.event().occurredAt(),
                context.event().observedAt(),
                toJson(data),
                CorrelationIds.current()
        ));

        log.debug("Dispatched TrassirVideoEventReceived: serverId={}, objectKey={}, subtype={}",
                context.serverId(), objectKey, subtype);
        return EventProcessingResult.success();
    }

    private String text(JsonNode node, String field) {
        JsonNode v = node.get(field);
        return v != null && v.isString() && !v.asString().isBlank() ? v.asString() : null;
    }

    private String toJson(Object node) {
        if (node == null) return "{}";
        try {
            return objectMapper.writeValueAsString(node);
        } catch (Exception e) {
            log.warn("Cannot serialize video_event payload", e);
            return "{}";
        }
    }
}