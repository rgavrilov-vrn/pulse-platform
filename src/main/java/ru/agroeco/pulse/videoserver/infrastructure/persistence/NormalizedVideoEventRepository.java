package ru.agroeco.pulse.videoserver.infrastructure.persistence;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import ru.agroeco.pulse.videoserver.domain.NormalizedVideoEvent;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

public interface NormalizedVideoEventRepository
        extends JpaRepository<NormalizedVideoEvent, UUID>,
        JpaSpecificationExecutor<NormalizedVideoEvent> {

    Optional<NormalizedVideoEvent> findByRawEventId(UUID rawEventId);

    // сортировка управляется через Pageable, не через имя метода
    Page<NormalizedVideoEvent> findByVideoServerId(UUID videoServerId, Pageable pageable);

    Page<NormalizedVideoEvent> findByCameraId(UUID cameraId, Pageable pageable);

    Page<NormalizedVideoEvent> findByVideoServerIdAndOccurredAtBetween(
            UUID videoServerId, Instant from, Instant to, Pageable pageable);

}