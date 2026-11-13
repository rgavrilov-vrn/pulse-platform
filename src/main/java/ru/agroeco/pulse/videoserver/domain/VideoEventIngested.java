package ru.agroeco.pulse.videoserver.domain;

import ru.agroeco.pulse.platform.domain.event.DomainEvent;
import ru.agroeco.pulse.platform.domain.event.DomainEventType;
import ru.agroeco.pulse.videoserver.api.EventSeverity;
import ru.agroeco.pulse.videoserver.api.VideoEventType;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public record VideoEventIngested(
        UUID normalizedEventId,
        UUID sourceEventId,        // = trassir.events.event_id
        UUID videoServerId,
        UUID cameraId,
        UUID channelId,
        Instant occurredAt,
        String source,
        VideoEventType videoEventType,
        EventSeverity severity
) implements DomainEvent {

    @Override public DomainEventType eventType() { return DomainEventType.VIDEO_EVENT_INGESTED; }
    @Override public UUID entityId()              { return normalizedEventId; }
    @Override public String entitySchema()        { return "videoserver"; }
    @Override public String entityTable()         { return "normalized_video_events"; }
    @Override public Instant occurredAt()         { return occurredAt; }

    @Override
    public Object newDataSnapshot() {
        Map<String, Object> data = new HashMap<>();
        data.put("sourceEventId", sourceEventId);
        data.put("videoServerId", videoServerId);
        data.put("cameraId", cameraId);
        data.put("channelId", channelId);
        data.put("source", source);
        data.put("eventType", videoEventType.name());
        data.put("severity", severity.name());
        return data;
    }
}