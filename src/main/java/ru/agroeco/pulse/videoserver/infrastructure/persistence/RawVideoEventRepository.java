package ru.agroeco.pulse.videoserver.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.agroeco.pulse.videoserver.domain.RawVideoEvent;

import java.util.Optional;
import java.util.UUID;

public interface RawVideoEventRepository extends JpaRepository<RawVideoEvent, UUID> {

    Optional<RawVideoEvent> findByVideoServerIdAndExternalEventId(
            UUID videoServerId, String externalEventId);
}