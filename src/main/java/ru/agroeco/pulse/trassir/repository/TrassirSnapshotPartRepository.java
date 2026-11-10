package ru.agroeco.pulse.trassir.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import ru.agroeco.pulse.trassir.domain.TrassirSnapshotPart;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface TrassirSnapshotPartRepository extends JpaRepository<TrassirSnapshotPart, UUID> {

    long countBySnapshotId(UUID snapshotId);

    List<TrassirSnapshotPart> findBySnapshotIdOrderByPartIndex(UUID snapshotId);

    /** FIX #28: атомарная вставка part'а — concurrent доставка одного part'а не падает. */
    @Modifying
    @Query(value = """
        INSERT INTO trassir.snapshot_parts
            (id, snapshot_id, part_index, payload, received_at)
        VALUES
            (:id, :snapshotId, :partIndex, CAST(:payload AS jsonb), :receivedAt)
        ON CONFLICT (snapshot_id, part_index) DO NOTHING
        """, nativeQuery = true)
    int insertIfAbsent(@Param("id") UUID id,
                       @Param("snapshotId") UUID snapshotId,
                       @Param("partIndex") Integer partIndex,
                       @Param("payload") String payload,
                       @Param("receivedAt") Instant receivedAt);

    @Modifying
    @Query("delete from TrassirSnapshotPart part")
    int deleteAllParts();
}