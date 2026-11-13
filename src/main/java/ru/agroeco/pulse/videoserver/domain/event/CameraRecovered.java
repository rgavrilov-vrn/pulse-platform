package ru.agroeco.pulse.videoserver.domain.event;

import lombok.Builder;
import ru.agroeco.pulse.platform.domain.event.DomainEvent;
import ru.agroeco.pulse.platform.domain.event.DomainEventType;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Builder
public record CameraRecovered(
        UUID cameraId,
        UUID videoServerId,
        String externalId,
        String cameraName,
        Instant occurredAt
) implements DomainEvent {
    @Override public DomainEventType eventType() { return DomainEventType.CAMERA_RECOVERED; }
    @Override public UUID entityId()              { return cameraId; }
    @Override public String entitySchema()        { return "videoserver"; }
    @Override public String entityTable()         { return "cameras"; }
    @Override public Instant occurredAt()         { return occurredAt; }
    @Override public boolean publishToOutbox()    { return true; }
    @Override public Object newDataSnapshot() {
        Map<String, Object> d = new HashMap<>();
        d.put("externalId", externalId);
        d.put("cameraName", cameraName);
        return d;
    }
}