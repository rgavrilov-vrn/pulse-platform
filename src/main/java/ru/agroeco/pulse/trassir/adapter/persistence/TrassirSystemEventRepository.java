package ru.agroeco.pulse.trassir.adapter.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import ru.agroeco.pulse.trassir.domain.TrassirSystemEvent;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface TrassirSystemEventRepository
        extends JpaRepository<TrassirSystemEvent, UUID>,
        JpaSpecificationExecutor<TrassirSystemEvent> {

    // ❌ search(...) @Query убран — используется Specification (без ? IS NULL)

    @Query("""
            SELECT DISTINCT e.eventType FROM TrassirSystemEvent e
            WHERE e.serverId = coalesce(:serverId, e.serverId)
            ORDER BY e.eventType
            """)
    List<String> distinctEventTypes(@Param("serverId") UUID serverId);

    @Query("""
            SELECT DISTINCT e.severity FROM TrassirSystemEvent e
            WHERE e.serverId = coalesce(:serverId, e.serverId)
            ORDER BY e.severity
            """)
    List<String> distinctSeverities(@Param("serverId") UUID serverId);

    // aggregates: from/to всегда non-null (service подставляет Instant.EPOCH / MAX)
    @Query("""
            SELECT e.eventType, count(e) FROM TrassirSystemEvent e
            WHERE e.serverId = coalesce(:serverId, e.serverId)
              AND e.observedAt >= :from
              AND e.observedAt <= :to
            GROUP BY e.eventType
            """)
    List<Object[]> countByEventType(@Param("serverId") UUID serverId,
                                    @Param("from") Instant from,
                                    @Param("to") Instant to);

    @Query("""
            SELECT e.severity, count(e) FROM TrassirSystemEvent e
            WHERE e.serverId = coalesce(:serverId, e.serverId)
              AND e.observedAt >= :from
              AND e.observedAt <= :to
            GROUP BY e.severity
            """)
    List<Object[]> countBySeverity(@Param("serverId") UUID serverId,
                                   @Param("from") Instant from,
                                   @Param("to") Instant to);

    @Query("""
            SELECT e.objectId, count(e) FROM TrassirSystemEvent e
            WHERE e.serverId = coalesce(:serverId, e.serverId)
              AND e.objectId IS NOT NULL
              AND e.observedAt >= :from
              AND e.observedAt <= :to
            GROUP BY e.objectId
            ORDER BY count(e) DESC
            """)
    List<Object[]> countByObjectId(@Param("serverId") UUID serverId,
                                   @Param("from") Instant from,
                                   @Param("to") Instant to);

    @Modifying
    @Query("delete from TrassirSystemEvent e where e.observedAt < :cutoff")
    int deleteOlderThan(@Param("cutoff") Instant cutoff);

    @Modifying
    @Query("delete from TrassirSystemEvent event")
    int deleteAllSystemEvents();
}