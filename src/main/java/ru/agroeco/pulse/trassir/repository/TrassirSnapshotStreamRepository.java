package ru.agroeco.pulse.trassir.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import ru.agroeco.pulse.trassir.domain.TrassirSnapshotStream;

import java.util.Optional;
import java.util.UUID;

public interface TrassirSnapshotStreamRepository extends JpaRepository<TrassirSnapshotStream, UUID> {
    Optional<TrassirSnapshotStream> findByServerIdAndStreamId(String serverId, String streamId);

    @Modifying
    @Query("delete from TrassirSnapshotStream stream")
    int deleteAllStreams();
}
