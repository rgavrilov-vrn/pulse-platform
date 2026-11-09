package ru.agroeco.pulse.platform.trassir.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.agroeco.pulse.platform.trassir.domain.TrassirSnapshot;

import java.util.Optional;
import java.util.UUID;

public interface TrassirSnapshotRepository extends JpaRepository<TrassirSnapshot, UUID> {

    Optional<TrassirSnapshot> findByServerIdAndStreamIdAndSnapshotSeq(
            String serverId,
            String streamId,
            Long snapshotSeq
    );
}
