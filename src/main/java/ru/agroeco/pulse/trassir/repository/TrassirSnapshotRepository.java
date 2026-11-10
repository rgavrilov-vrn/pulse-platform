package ru.agroeco.pulse.trassir.repository;

import org.springframework.data.jpa.repository.JpaRepository;
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
            String serverId,
            UUID externalSnapshotId
    );

    long countByStatus(String status);

    List<TrassirSnapshot> findTop50ByStatusAndNextRetryAtBeforeOrderByNextRetryAtAsc(
            String status,
            Instant nextRetryAt
    );

    @Modifying
    @Query("update TrassirSnapshot snapshot set snapshot.completedAt = :completedAt, snapshot.status = 'COMPLETED', snapshot.lastError = null, snapshot.nextRetryAt = null where snapshot.id = :id and snapshot.completedAt is null")
    int completeIfPending(@Param("id") UUID id, @Param("completedAt") Instant completedAt);

    @Modifying
    @Query("update TrassirSnapshot snapshot set snapshot.status = 'FAILED', snapshot.lastError = :error, snapshot.retryCount = snapshot.retryCount + 1, snapshot.nextRetryAt = :nextRetryAt where snapshot.id = :id and snapshot.completedAt is null")
    int markFailed(@Param("id") UUID id, @Param("error") String error, @Param("nextRetryAt") Instant nextRetryAt);

    @Modifying
    @Query("update TrassirSnapshot snapshot set snapshot.status = 'DEAD_LETTER', snapshot.nextRetryAt = null where snapshot.id = :id and snapshot.completedAt is null")
    int markDeadLetter(@Param("id") UUID id);

    @Modifying
    @Query("delete from TrassirSnapshot snapshot")
    int deleteAllSnapshots();
}
