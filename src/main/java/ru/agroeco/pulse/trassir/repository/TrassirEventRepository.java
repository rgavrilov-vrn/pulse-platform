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
     * FIX #27: идемпотентная вставка события. Если event_id уже есть
     * (агент потерял state и выслал новый batch с теми же событиями) —
     * пропускаем без ошибки.
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



    Optional<TrassirEvent> findById(UUID id);

    @Modifying
    @Query("delete from TrassirEvent event where event.createdAt < :cutoff and event.status in ('PROCESSED', 'DEAD_LETTER')")
    int deleteFinishedOlderThan(@Param("cutoff") Instant cutoff);

    @Modifying
    @Query("delete from TrassirEvent event")
    int deleteAllEvents();

    /** FIX #17, #77: только ID кандидатов — БЕЗ FOR UPDATE. */
    @Query(value = """
    SELECT id FROM trassir.events
    WHERE (status = 'FAILED' AND next_retry_at <= :now)
       OR (status = 'PENDING' AND created_at <= :staleCutoff)
    ORDER BY COALESCE(next_retry_at, created_at), id
    LIMIT :limit
    """, nativeQuery = true)
    List<UUID> findCandidateIdsForRetry(@Param("now") Instant now,
                                        @Param("staleCutoff") Instant staleCutoff,
                                        @Param("limit") int limit);

    /** FIX #17, #77: атомарная блокировка одного события с SKIP LOCKED. */
    @Query(value = """
    SELECT * FROM trassir.events
    WHERE id = :id
      AND (
        (status = 'FAILED' AND next_retry_at <= :now)
        OR (status = 'PENDING' AND created_at <= :staleCutoff)
      )
    FOR UPDATE SKIP LOCKED
    """, nativeQuery = true)
    Optional<TrassirEvent> lockByIdForRetry(@Param("id") UUID id,
                                            @Param("now") Instant now,
                                            @Param("staleCutoff") Instant staleCutoff);
}