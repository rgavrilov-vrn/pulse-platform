package ru.agroeco.pulse.videoserver.infrastructure.persistence;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import ru.agroeco.pulse.videoserver.domain.RawVideoEvent;

public interface RawVideoEventRepository extends JpaRepository<RawVideoEvent, UUID> {
}
