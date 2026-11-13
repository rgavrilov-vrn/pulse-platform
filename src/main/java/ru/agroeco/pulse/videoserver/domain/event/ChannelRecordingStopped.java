package ru.agroeco.pulse.videoserver.domain.event;

import lombok.Builder;
import ru.agroeco.pulse.platform.domain.event.DomainEvent;
import ru.agroeco.pulse.platform.domain.event.DomainEventType;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Builder
public record ChannelRecordingStopped(
        UUID channelId,
        UUID cameraId,
        UUID videoServerId,
        String channelExternalId,
        String channelName,
        Instant occurredAt
) implements DomainEvent {
    @Override public DomainEventType eventType() { return DomainEventType.CHANNEL_RECORDING_STOPPED; }
    @Override public UUID entityId()              { return channelId; }
    @Override public String entitySchema()        { return "videoserver"; }
    @Override public String entityTable()         { return "channels"; }
    @Override public Instant occurredAt()         { return occurredAt; }
    @Override public boolean publishToOutbox()    { return true; }
    @Override public Object newDataSnapshot() {
        Map<String, Object> d = new HashMap<>();
        d.put("cameraId", cameraId.toString());
        d.put("channelExternalId", channelExternalId);
        d.put("channelName", channelName);
        return d;
    }
}