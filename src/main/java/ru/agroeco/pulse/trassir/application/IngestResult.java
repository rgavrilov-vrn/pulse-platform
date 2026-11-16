package ru.agroeco.pulse.trassir.application;

import java.util.UUID;

/**
 * Результат обработки одного ingest-envelope.
 * Возвращается use case → используется контроллером для логирования.
 */
public record IngestResult(
        UUID batchId,
        UUID rawId,
        UUID serverId,
        int eventCount,
        int acceptedCount,
        boolean duplicate,
        String status,
        String message
) {
    public static IngestResult accepted(UUID batchId, UUID rawId, UUID serverId,
                                        int eventCount, int acceptedCount) {
        return new IngestResult(batchId, rawId, serverId, eventCount, acceptedCount,
                false, "ACCEPTED", "Batch processed");
    }

    public static IngestResult duplicate(UUID batchId, UUID rawId, UUID serverId) {
        return new IngestResult(batchId, rawId, serverId, 0, 0,
                true, "DUPLICATE_BATCH", "Batch already processed");
    }
}