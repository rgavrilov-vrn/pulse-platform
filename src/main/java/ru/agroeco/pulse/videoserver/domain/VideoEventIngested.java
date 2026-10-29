package ru.agroeco.pulse.videoserver.domain;



import ru.agroeco.pulse.audit.domain.event.AuditAction;
import ru.agroeco.pulse.platform.domain.event.DomainEvent;
import ru.agroeco.pulse.videoserver.api.EventSeverity;
import ru.agroeco.pulse.videoserver.api.VideoEventType;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public record VideoEventIngested(
        UUID normalizedEventId,
        UUID rawEventId,
        UUID videoServerId,
        UUID cameraId,
        UUID channelId,
        Instant occurredAt,
        String source,
        VideoEventType videoEventType,   // ← было eventType
        EventSeverity severity
) implements DomainEvent {

    @Override
    public AuditAction eventType() {
        return AuditAction.VIDEO_EVENT_INGESTED;
    }

    @Override
    public UUID entityId() {
        return normalizedEventId;
    }

    @Override
    public String entitySchema() {
        return "videoserver";
    }

    @Override
    public String entityTable() {
        return "normalized_video_events";
    }

    @Override
    public Object newDataSnapshot() {
        Map<String, Object> data = new HashMap<>();
        data.put("rawEventId", rawEventId);
        data.put("videoServerId", videoServerId);
        data.put("cameraId", cameraId);
        data.put("channelId", channelId);
        data.put("source", source);
        data.put("eventType", videoEventType.name());   // ← videoEventType
        data.put("severity", severity.name());
        return data;
    }
}