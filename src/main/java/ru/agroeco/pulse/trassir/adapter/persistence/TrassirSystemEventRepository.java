package ru.agroeco.pulse.trassir.adapter.persistence;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import ru.agroeco.pulse.trassir.domain.TrassirSystemEvent;

import java.time.Instant;
import java.util.UUID;

public interface TrassirSystemEventRepository extends JpaRepository<TrassirSystemEvent, UUID> {

    @Query("""
            SELECT e FROM TrassirSystemEvent e
            WHERE (:serverId IS NULL OR e.serverId = :serverId)
              AND (:eventType IS NULL OR e.eventType = :eventType)
              AND (:severity IS NULL OR e.severity = :severity)
              AND (:from IS NULL OR e.observedAt >= :from)
              AND (:to   IS NULL OR e.observedAt <= :to)
            """)
    Page<TrassirSystemEvent> search(@Param("serverId") UUID serverId,
                                    @Param("eventType") String eventType,
                                    @Param("severity") String severity,
                                    @Param("from") Instant from,
                                    @Param("to") Instant to,
                                    Pageable pageable);

    @Modifying
    @Query("delete from TrassirSystemEvent event")
    int deleteAllSystemEvents();
}