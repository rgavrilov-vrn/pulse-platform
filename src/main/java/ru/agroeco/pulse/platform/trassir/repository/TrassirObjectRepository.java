package ru.agroeco.pulse.platform.trassir.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.agroeco.pulse.platform.trassir.domain.TrassirObject;

import java.util.Optional;
import java.util.UUID;

public interface TrassirObjectRepository extends JpaRepository<TrassirObject, UUID> {

    Optional<TrassirObject> findByServerIdAndObjectKey(String serverId, String objectKey);
}
