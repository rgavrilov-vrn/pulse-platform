package ru.agroeco.pulse.trassir.api.event;

import ru.agroeco.pulse.platform.domain.event.DomainEventType;
import ru.agroeco.pulse.platform.domain.event.IntegrationEvent;

import java.time.Instant;
import java.util.UUID;

/**
 * Публикуется, когда из TRASSIR приходит системное событие
 * "Connection Lost" или "Connection Established".
 * Потребитель: videoserver (создание domain event CameraOffline / CameraOnline).
 */
public record TrassirCameraConnectionChanged(
        UUID eventId,
        int schemaVersion,
        UUID serverId,
        String cameraExternalId,
        String cameraName,
        boolean connected,
        Instant occurredAt,
        String correlationId
) implements IntegrationEvent {

    public TrassirCameraConnectionChanged {
        if (schemaVersion <= 0) schemaVersion = 1;
    }

    @Override public DomainEventType eventType() { return DomainEventType.TRASSIR_CAMERA_CONNECTION_CHANGED; }
    @Override public UUID entityId()              { return eventId; }
    @Override public String entitySchema()        { return "trassir"; }
    @Override public String entityTable()         { return "system_events"; }
    @Override public Instant occurredAt()         { return occurredAt; }
    @Override public boolean publishToOutbox()    { return false; }
}