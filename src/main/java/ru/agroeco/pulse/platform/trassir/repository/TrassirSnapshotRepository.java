package ru.agroeco.pulse.platform.trassir.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import ru.agroeco.pulse.platform.trassir.domain.TrassirSnapshot;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

public interface TrassirSnapshotRepository extends JpaRepository<TrassirSnapshot, UUID> {

    Optional<TrassirSnapshot> findByServerIdAndStreamIdAndSnapshotSeq(
            String serverId,
            String streamId,
            Long snapshotSeq
    );

    @Modifying
    @Query("""
            update TrassirSnapshot snapshot
            set snapshot.completedAt = :completedAt
            where snapshot.id = :id
              and snapshot.completedAt is null
            """)
    int completeIfPending(@Param("id") UUID id, @Param("completedAt") Instant completedAt);
}
