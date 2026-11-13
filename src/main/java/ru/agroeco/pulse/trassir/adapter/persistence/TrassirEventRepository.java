package ru.agroeco.pulse.trassir.adapter.persistence;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
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
                       @Param("serverId") UUID serverId,
                       @Param("eventIndex") Integer eventIndex,
                       @Param("eventId") UUID eventId,
                       @Param("eventType") String eventType,
                       @Param("eventVersion") String eventVersion,
                       @Param("occurredAt") Instant occurredAt,
                       @Param("observedAt") Instant observedAt,
                       @Param("source") String source,
                       @Param("data") String data,
                       @Param("rawEvent") String rawEvent);

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

    @Query("""
            SELECT e FROM TrassirEvent e
            WHERE (:serverId IS NULL OR e.serverId = :serverId)
              AND (:eventType IS NULL OR e.eventType = :eventType)
              AND (:status IS NULL OR e.status = :status)
              AND (:from IS NULL OR e.createdAt >= :from)
              AND (:to   IS NULL OR e.createdAt <= :to)
            """)
    Page<TrassirEvent> search(@Param("serverId") UUID serverId,
                              @Param("eventType") String eventType,
                              @Param("status") String status,
                              @Param("from") Instant from,
                              @Param("to") Instant to,
                              Pageable pageable);

    @Modifying
    @Query("delete from TrassirEvent event where event.createdAt < :cutoff and event.status in ('PROCESSED','DEAD_LETTER')")
    int deleteFinishedOlderThan(@Param("cutoff") Instant cutoff);

    @Modifying
    @Query("delete from TrassirEvent event")
    int deleteAllEvents();

    @Override
    Optional<TrassirEvent> findById(UUID id);
}