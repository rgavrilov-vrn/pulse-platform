package ru.agroeco.pulse.videoserver.adapter.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import ru.agroeco.pulse.videoserver.domain.VideoServer;

import java.util.UUID;

public interface VideoServerRepository
        extends JpaRepository<VideoServer, UUID>,
        JpaSpecificationExecutor<VideoServer> {
}