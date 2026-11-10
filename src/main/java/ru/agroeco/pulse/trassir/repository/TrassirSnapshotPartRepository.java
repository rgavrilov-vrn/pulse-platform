package ru.agroeco.pulse.trassir.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import ru.agroeco.pulse.trassir.domain.TrassirSnapshotPart;

import java.util.List;
import java.util.UUID;

public interface TrassirSnapshotPartRepository extends JpaRepository<TrassirSnapshotPart, UUID> {
    boolean existsBySnapshotIdAndPartIndex(UUID snapshotId, Integer partIndex);
    long countBySnapshotId(UUID snapshotId);
    List<TrassirSnapshotPart> findBySnapshotIdOrderByPartIndex(UUID snapshotId);

    @Modifying
    @Query("delete from TrassirSnapshotPart part")
    int deleteAllParts();
}
