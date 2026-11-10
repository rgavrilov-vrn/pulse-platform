package ru.agroeco.pulse.platform.trassir.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import ru.agroeco.pulse.platform.trassir.domain.TrassirSnapshotPart;

import java.util.UUID;

public interface TrassirSnapshotPartRepository extends JpaRepository<TrassirSnapshotPart, UUID> {

    @Modifying
    @Query("delete from TrassirSnapshotPart part")
    int deleteAllParts();
}
