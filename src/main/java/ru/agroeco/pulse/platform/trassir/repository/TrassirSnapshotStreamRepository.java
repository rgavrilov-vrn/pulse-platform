package ru.agroeco.pulse.platform.trassir.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import ru.agroeco.pulse.platform.trassir.domain.TrassirSnapshotStream;

import java.util.UUID;

public interface TrassirSnapshotStreamRepository extends JpaRepository<TrassirSnapshotStream, UUID> {

    @Modifying
    @Query("delete from TrassirSnapshotStream stream")
    int deleteAllStreams();
}
