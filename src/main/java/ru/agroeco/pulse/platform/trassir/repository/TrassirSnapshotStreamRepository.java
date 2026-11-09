package ru.agroeco.pulse.platform.trassir.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.agroeco.pulse.platform.trassir.domain.TrassirSnapshotStream;

import java.util.Optional;
import java.util.UUID;

public interface TrassirSnapshotStreamRepository extends JpaRepository<TrassirSnapshotStream, UUID> {

    Optional<TrassirSnapshotStream> findByServerIdAndStreamId(String serverId, String streamId);
}
