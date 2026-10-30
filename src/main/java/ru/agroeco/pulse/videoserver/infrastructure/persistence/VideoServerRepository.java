package ru.agroeco.pulse.videoserver.infrastructure.persistence;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import ru.agroeco.pulse.videoserver.domain.VideoServer;

public interface VideoServerRepository
        extends JpaRepository<VideoServer, UUID>,
        JpaSpecificationExecutor<VideoServer> {
}
