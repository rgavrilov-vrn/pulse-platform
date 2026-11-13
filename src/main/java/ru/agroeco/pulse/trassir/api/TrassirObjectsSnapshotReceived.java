package ru.agroeco.pulse.trassir.api.event;

import ru.agroeco.pulse.platform.domain.event.DomainEventType;
import ru.agroeco.pulse.platform.domain.event.IntegrationEvent;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Публикуется после успешной обработки {@code agent.objects_snapshot}.
 * Потребитель: videoserver (reconciliation inventory).
 *
 * <p>Kafka-ready. Topic (future): {@code pulse.trassir.objects-snapshot-received}.</p>
 */
public record TrassirObjectsSnapshotReceived(
        UUID eventId,               // = trassir.snapshots.id (dedup key)
        int schemaVersion,
        UUID serverId,              // = video_servers.id
        UUID snapshotId,            // = trassir.snapshots.id
        Instant receivedAt,
        List<SnapshotObject> objects,
        String correlationId
) implements IntegrationEvent {

    public TrassirObjectsSnapshotReceived {
        if (schemaVersion <= 0) schemaVersion = 1;
        objects = objects == null ? List.of() : List.copyOf(objects);
    }

    @Override public DomainEventType eventType() { return DomainEventType.TRASSIR_OBJECTS_SNAPSHOT_RECEIVED; }
    @Override public UUID entityId()              { return snapshotId; }
    @Override public String entitySchema()        { return "trassir"; }
    @Override public String entityTable()         { return "snapshots"; }
    @Override public Instant occurredAt()         { return receivedAt; }
    @Override public boolean publishToOutbox()    { return false; }   // IPC, не audit/Kafka

    /**
     * Один объект из snapshot.
     *
     * @param objectKey   = videoserver.cameras.external_id
     * @param payloadJson raw JSON из trassir.snapshot_objects.object_payload
     */
    public record SnapshotObject(String objectKey, String payloadJson) {}
}