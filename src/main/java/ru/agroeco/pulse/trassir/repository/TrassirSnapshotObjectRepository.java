package ru.agroeco.pulse.trassir.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import ru.agroeco.pulse.trassir.domain.TrassirSnapshotObject;

import java.util.UUID;

public interface TrassirSnapshotObjectRepository extends JpaRepository<TrassirSnapshotObject, UUID> {

    @Modifying
    @Query(value = """
        INSERT INTO trassir.snapshot_objects (id, snapshot_id, object_key, object_payload)
        VALUES (:id, :snapshotId, :objectKey, CAST(:objectPayload AS jsonb))
        ON CONFLICT (snapshot_id, object_key) DO NOTHING
        """, nativeQuery = true)
    int insertIfAbsent(@Param("id") UUID id,
                       @Param("snapshotId") UUID snapshotId,
                       @Param("objectKey") String objectKey,
                       @Param("objectPayload") String objectPayload);

    @Modifying
    @Query("delete from TrassirSnapshotObject object")
    int deleteAllObjects();
}