package ru.agroeco.pulse.platform.trassir.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.agroeco.pulse.platform.trassir.domain.TrassirSnapshotPart;

import java.util.List;
import java.util.UUID;

public interface TrassirSnapshotPartRepository extends JpaRepository<TrassirSnapshotPart, UUID> {

    long countBySnapshotId(UUID snapshotId);

    List<TrassirSnapshotPart> findBySnapshotIdOrderByPartIndex(UUID snapshotId);
}
