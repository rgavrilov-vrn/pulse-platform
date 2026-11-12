package ru.agroeco.pulse.trassir.adapter.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import ru.agroeco.pulse.trassir.domain.TrassirObject;

import java.util.Optional;
import java.util.UUID;

public interface TrassirObjectRepository extends JpaRepository<TrassirObject, UUID> {
    Optional<TrassirObject> findByServerIdAndObjectKey(String serverId, String objectKey);

    @Modifying
    @Query("delete from TrassirObject object")
    int deleteAllObjects();
}
