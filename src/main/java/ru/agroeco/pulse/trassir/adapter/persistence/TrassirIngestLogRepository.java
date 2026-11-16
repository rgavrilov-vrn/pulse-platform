package ru.agroeco.pulse.trassir.adapter.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import ru.agroeco.pulse.trassir.domain.TrassirIngestLog;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface TrassirIngestLogRepository
        extends JpaRepository<TrassirIngestLog, UUID>,
        JpaSpecificationExecutor<TrassirIngestLog> {

    List<TrassirIngestLog> findByBatchIdOrderByReceivedAtDesc(UUID batchId);

    @Query("""
            SELECT l.httpStatus, count(l) FROM TrassirIngestLog l
            WHERE l.receivedAt >= :from AND l.receivedAt <= :to
            GROUP BY l.httpStatus
            """)
    List<Object[]> countByHttpStatus(@Param("from") Instant from, @Param("to") Instant to);

    @Query("""
            SELECT l.responseCode, count(l) FROM TrassirIngestLog l
            WHERE l.responseCode IS NOT NULL
              AND l.receivedAt >= :from AND l.receivedAt <= :to
            GROUP BY l.responseCode
            ORDER BY count(l) DESC
            """)
    List<Object[]> countByResponseCode(@Param("from") Instant from, @Param("to") Instant to);

    @Modifying
    @Query("delete from TrassirIngestLog l where l.receivedAt < :cutoff")
    int deleteOlderThan(@Param("cutoff") Instant cutoff);
}