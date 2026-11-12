package ru.agroeco.pulse.trassir.adapter.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import ru.agroeco.pulse.trassir.domain.TrassirIngestRaw;

import java.time.Instant;
import java.util.UUID;

public interface TrassirIngestRawRepository extends JpaRepository<TrassirIngestRaw, UUID> {

    boolean existsByBatchId(UUID batchId);

    /**
     * FIX #27: атомарная вставка с идемпотентностью по batch_id.
     * Возвращает 1 при успехе, 0 если батч уже есть (race-safe).
     */
    @Modifying
    @Query(value = """
        INSERT INTO trassir.ingest_raw
            (id, batch_id, received_at, sent_at, producer_name, producer_version,
             server_id, raw_body, created_at)
        VALUES
            (:id, :batchId, :receivedAt, :sentAt, :producerName, :producerVersion,
             :serverId, CAST(:rawBody AS jsonb), now())
        ON CONFLICT (batch_id) DO NOTHING
        """, nativeQuery = true)
    int insertIfAbsent(@Param("id") UUID id,
                       @Param("batchId") UUID batchId,
                       @Param("receivedAt") Instant receivedAt,
                       @Param("sentAt") Instant sentAt,
                       @Param("producerName") String producerName,
                       @Param("producerVersion") String producerVersion,
                       @Param("serverId") String serverId,
                       @Param("rawBody") String rawBody);

    @Modifying
    @Query("delete from TrassirIngestRaw raw where raw.createdAt < :cutoff")
    int deleteOlderThan(@Param("cutoff") Instant cutoff);

    @Modifying
    @Query("delete from TrassirIngestRaw raw")
    int deleteAllRaw();
}