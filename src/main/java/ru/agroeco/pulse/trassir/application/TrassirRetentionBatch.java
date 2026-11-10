package ru.agroeco.pulse.trassir.application;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;
import java.time.Instant;

/**
 * FIX #135: батч-DELETE. Один DELETE FROM на 10M строк — долгая транзакция,
 * огромный WAL, autovacuum-шторм. Режем на порции по 1000.
 *
 * REQUIRES_NEW на каждый батч — короткие транзакции, WAL сбрасывается.
 */
@Component
@RequiredArgsConstructor
public class TrassirRetentionBatch {

    private final JdbcTemplate jdbc;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public int deleteEventBatch(Instant cutoff, int limit) {
        Integer deleted = jdbc.queryForObject("""
            WITH victims AS (
                SELECT id FROM trassir.events
                WHERE created_at < ? AND status IN ('PROCESSED','DEAD_LETTER')
                LIMIT ?
            )
            DELETE FROM trassir.events e
            USING victims v
            WHERE e.id = v.id
            """, Integer.class, Timestamp.from(cutoff), limit);
        return deleted == null ? 0 : deleted;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public int deleteRawBatch(Instant cutoff, int limit) {
        Integer deleted = jdbc.queryForObject("""
            WITH victims AS (
                SELECT id FROM trassir.ingest_raw
                WHERE created_at < ?
                LIMIT ?
            )
            DELETE FROM trassir.ingest_raw r
            USING victims v
            WHERE r.id = v.id
            """, Integer.class, Timestamp.from(cutoff), limit);
        return deleted == null ? 0 : deleted;
    }
}