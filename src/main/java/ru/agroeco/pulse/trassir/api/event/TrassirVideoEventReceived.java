package ru.agroeco.pulse.trassir.api.event;

import ru.agroeco.pulse.platform.domain.event.DomainEventType;
import ru.agroeco.pulse.platform.domain.event.IntegrationEvent;

import java.time.Instant;
import java.util.UUID;

/**
 * Публикуется после обработки {@code trassir.video_event}.
 * Потребитель: videoserver (ingest в normalized_video_events).
 *
 * <p>Kafka-ready. Topic (future): {@code pulse.trassir.video-event-received}.</p>
 *
 * <p>Контракт envelope от агента:</p>
 * <pre>
 * {"event_type":"trassir.video_event",
 *  "data":{"object_id":"cam-01","channel_id":"ch-01","subtype":"MOTION","severity":"WARNING"}}
 * </pre>
 *
 * <p><b>ВАЖНО:</b> record-компонент {@code videoEventType} (не {@code eventType}),
 * потому что {@link #eventType()} должен возвращать {@link DomainEventType} согласно
 * контракту {@link ru.agroeco.pulse.platform.domain.event.DomainEvent}.
 * Java запрещает record-компоненту иметь то же имя, что и метод интерфейса,
 * если типы не совпадают.</p>
 */
public record TrassirVideoEventReceived(
        UUID eventId,               // = trassir.events.event_id (dedup key)
        int schemaVersion,
        UUID serverId,              // = video_servers.id
        String objectKey,           // = cameras.external_id
        String channelExternalId,   // nullable
        String videoEventType,      // ← БЫЛО eventType. MOTION | OFFLINE | TAMPERING | ...
        String severity,            // INFO | WARNING | CRITICAL
        Instant occurredAt,
        Instant observedAt,
        String payloadJson,
        String correlationId
) implements IntegrationEvent {

    public TrassirVideoEventReceived {
        if (schemaVersion <= 0) schemaVersion = 1;
    }

    @Override public DomainEventType eventType() { return DomainEventType.TRASSIR_VIDEO_EVENT_RECEIVED; }
    @Override public UUID entityId()              { return eventId; }
    @Override public String entitySchema()        { return "trassir"; }
    @Override public String entityTable()         { return "events"; }
    @Override public Instant occurredAt()         { return occurredAt != null ? occurredAt : observedAt; }
    @Override public boolean publishToOutbox()    { return false; }
}