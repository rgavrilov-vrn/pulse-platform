package ru.agroeco.pulse.trassir.repository;

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
            String serverId, UUID externalSnapshotId);

    // FIX: @Lock — Spring Data JPA (org.springframework.data.jpa.repository.Lock);
    // LockModeType — Jakarta JPA (jakarta.persistence.LockModeType).
    // Раньше было @jakarta.persistence.Lock(...) — такого класса не существует.
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT s FROM TrassirSnapshot s WHERE s.id = :id")
    Optional<TrassirSnapshot> findByIdForUpdate(@Param("id") UUID id);

    /** FIX: используется TrassirSnapshotRetryWorker для метрики pending. */
    long countByStatus(String status);

    /**
     * FIX #27: атомарное создание. 0 — снапшот уже есть (другой part пришёл раньше).
     */
    @Modifying
    @Query(value = """
        INSERT INTO trassir.snapshots
            (id, external_snapshot_id, server_id, stream_id, snapshot_seq,
             total_parts, received_at, status, retry_count)
        VALUES
            (:id, :externalSnapshotId, :serverId, :serverId, 0,
             :totalParts, :receivedAt, 'PENDING', 0)
        ON CONFLICT (external_snapshot_id) DO NOTHING
        """, nativeQuery = true)
    int insertIfAbsent(@Param("id") UUID id,
                       @Param("externalSnapshotId") UUID externalSnapshotId,
                       @Param("serverId") String serverId,
                       @Param("totalParts") Integer totalParts,
                       @Param("receivedAt") Instant receivedAt);

    /**
     * FIX #29, #77: неблокирующая выборка ID кандидатов на retry.
     * Блокировка берётся в {@link #lockByIdForRetry} per-id — параллельные
     * инстансы не пересекаются.
     */
    @Query(value = """
        SELECT id FROM trassir.snapshots
        WHERE status = 'FAILED'
          AND next_retry_at <= :now
          AND completed_at IS NULL
        ORDER BY next_retry_at, id
        LIMIT :limit
        """, nativeQuery = true)
    List<UUID> findCandidateIdsForRetry(@Param("now") Instant now,
                                        @Param("limit") int limit);

    /**
     * FIX #29, #77: блокирующая выборка одной строки с SKIP LOCKED.
     * Пустой Optional — строка уже занята другим инстансом, статус изменился,
     * или completed_at установлен.
     */
    @Query(value = """
        SELECT * FROM trassir.snapshots
        WHERE id = :id
          AND status = 'FAILED'
          AND next_retry_at <= :now
          AND completed_at IS NULL
        FOR UPDATE SKIP LOCKED
        """, nativeQuery = true)
    Optional<TrassirSnapshot> lockByIdForRetry(@Param("id") UUID id,
                                               @Param("now") Instant now);

    @Modifying
    @Query("""
        update TrassirSnapshot s
           set s.completedAt = :completedAt,
               s.status = 'COMPLETED',
               s.lastError = null,
               s.nextRetryAt = null
         where s.id = :id and s.completedAt is null
        """)
    int completeIfPending(@Param("id") UUID id, @Param("completedAt") Instant completedAt);

    /** FIX #16: markFailed фиксируется в той же транзакции, не откатывается. */
    @Modifying
    @Query("""
        update TrassirSnapshot s
           set s.status = 'FAILED',
               s.lastError = :error,
               s.retryCount = s.retryCount + 1,
               s.nextRetryAt = :nextRetryAt
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

    @Modifying
    @Query("delete from TrassirSnapshot snapshot")
    int deleteAllSnapshots();
}