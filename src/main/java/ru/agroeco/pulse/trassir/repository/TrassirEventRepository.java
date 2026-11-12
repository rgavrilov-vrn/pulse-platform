package ru.agroeco.pulse.trassir.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import ru.agroeco.pulse.trassir.domain.TrassirEvent;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface TrassirEventRepository extends JpaRepository<TrassirEvent, UUID> {

    /**
     * Идемпотентная вставка события (FIX #27, партия 3).
     * Если event_id уже есть — пропускаем без ошибки.
     *
     * ВАЖНО: этот метод был случайно удалён при переписывании репозитория
     * в партии 7 (FIX #137) — восстановлен.
     */
    @Modifying
    @Query(value = """
        INSERT INTO trassir.events
            (id, raw_id, server_id, event_index, event_id, event_type, event_version,
             occurred_at, observed_at, source, data, raw_event, status, retry_count, created_at)
        VALUES
            (:id, :rawId, :serverId, :eventIndex, :eventId, :eventType, :eventVersion,
             :occurredAt, :observedAt, CAST(:source AS jsonb), CAST(:data AS jsonb),
             CAST(:rawEvent AS jsonb), 'PENDING', 0, now())
        ON CONFLICT (event_id) DO NOTHING
        """, nativeQuery = true)
    int insertIfAbsent(@Param("id") UUID id,
                       @Param("rawId") UUID rawId,
                       @Param("serverId") String serverId,
                       @Param("eventIndex") Integer eventIndex,
                       @Param("eventId") UUID eventId,
                       @Param("eventType") String eventType,
                       @Param("eventVersion") String eventVersion,
                       @Param("occurredAt") Instant occurredAt,
                       @Param("observedAt") Instant observedAt,
                       @Param("source") String source,
                       @Param("data") String data,
                       @Param("rawEvent") String rawEvent);

    /**
     * FIX #137: без FOR UPDATE. Раньше lockProcessable (FOR UPDATE SKIP LOCKED)
     * удерживал блокировки в outer-транзакции, а REQUIRES_NEW retry-worker'а
     * пытался UPDATE те же строки — deadlock. Конкурентность ограничивается
     * pg_try_advisory_xact_lock в TrassirEventProcessor.processById.
     */
    @Query(value = """
        SELECT * FROM trassir.events
        WHERE (status = 'FAILED' AND next_retry_at <= :now)
           OR (status = 'PENDING' AND created_at <= :staleCutoff)
        ORDER BY COALESCE(next_retry_at, created_at), id
        LIMIT 50
        """, nativeQuery = true)
    List<TrassirEvent> findProcessableCandidates(@Param("now") Instant now,
                                                 @Param("staleCutoff") Instant staleCutoff);

    @Query(value = "SELECT pg_try_advisory_xact_lock(hashtext(:key))", nativeQuery = true)
    Boolean tryAdvisoryLock(@Param("key") String key);

    @Modifying
    @Query("delete from TrassirEvent event where event.createdAt < :cutoff and event.status in ('PROCESSED','DEAD_LETTER')")
    int deleteFinishedOlderThan(@Param("cutoff") Instant cutoff);

    @Modifying
    @Query("delete from TrassirEvent event")
    int deleteAllEvents();

    @Override
    Optional<TrassirEvent> findById(UUID id);
}