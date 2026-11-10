package ru.agroeco.pulse.trassir.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import ru.agroeco.pulse.trassir.domain.TrassirSnapshotObject;

import java.util.UUID;

public interface TrassirSnapshotObjectRepository extends JpaRepository<TrassirSnapshotObject, UUID> {

    @Modifying
    @Query("delete from TrassirSnapshotObject object")
    int deleteAllObjects();
}
