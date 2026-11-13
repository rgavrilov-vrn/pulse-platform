package ru.agroeco.pulse.trassir.adapter.persistence;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import ru.agroeco.pulse.trassir.domain.TrassirSnapshot;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface TrassirSnapshotRepository extends JpaRepository<TrassirSnapshot, UUID> {

    Optional<TrassirSnapshot> findByServerIdAndExternalSnapshotId(
            UUID serverId, UUID externalSnapshotId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT s FROM TrassirSnapshot s WHERE s.id = :id")
    Optional<TrassirSnapshot> findByIdForUpdate(@Param("id") UUID id);

    @Modifying
    @Query(value = """
            INSERT INTO trassir.snapshots
                (id, external_snapshot_id, server_id, stream_id, snapshot_seq,
                total_parts, received_at, status, retry_count)
            VALUES
                (:id, :externalSnapshotId, :serverId, :serverId,
                -- FIX #139: epoch-millis — монотонно растёт, уникально для snapshot'ов
                -- одного stream'а. Раньше всегда было 0 (dead field).
                (EXTRACT(EPOCH FROM now()) * 1000)::bigint,
                :totalParts, :receivedAt, 'PENDING', 0)
            ON CONFLICT (external_snapshot_id) DO NOTHING
        """, nativeQuery = true)
    int insertIfAbsent(@Param("id") UUID id,
                       @Param("externalSnapshotId") UUID externalSnapshotId,
                       @Param("serverId") UUID serverId,
                       @Param("totalParts") Integer totalParts,
                       @Param("receivedAt") Instant receivedAt);

    /**
     * FIX #137: без FOR UPDATE — короткий read для retry-worker.
     * Конкурентность ограничивает advisory lock в assemble().
     */
    @Query(value = """
        SELECT * FROM trassir.snapshots
        WHERE status = 'FAILED' AND next_retry_at <= :now AND completed_at IS NULL
        ORDER BY next_retry_at, id
        LIMIT 50
        """, nativeQuery = true)
    List<TrassirSnapshot> findFailedForRetry(@Param("now") Instant now);

    @Query(value = "SELECT pg_try_advisory_xact_lock(hashtext(:key))", nativeQuery = true)
    Boolean tryAdvisoryLock(@Param("key") String key);

    @Modifying
    @Query("""
        update TrassirSnapshot s
           set s.completedAt = :completedAt, s.status = 'COMPLETED',
               s.lastError = null, s.nextRetryAt = null
         where s.id = :id and s.completedAt is null
        """)
    int completeIfPending(@Param("id") UUID id, @Param("completedAt") Instant completedAt);

    @Modifying
    @Query("""
        update TrassirSnapshot s
           set s.status = 'FAILED', s.lastError = :error,
               s.retryCount = s.retryCount + 1, s.nextRetryAt = :nextRetryAt
         where s.id = :id and s.completedAt is null
        """)
    int markFailed(@Param("id") UUID id,
                   @Param("error") String error,
                   @Param("nextRetryAt") Instant nextRetryAt);

    @Modifying
    @Query("""
        update TrassirSnapshot s
           set s.status = 'DEAD_LETTER', s.nextRetryAt = null
         where s.id = :id and s.completedAt is null
        """)
    int markDeadLetter(@Param("id") UUID id);

    long countByStatus(String status);

    @Modifying
    @Query("delete from TrassirSnapshot snapshot")
    int deleteAllSnapshots();
}