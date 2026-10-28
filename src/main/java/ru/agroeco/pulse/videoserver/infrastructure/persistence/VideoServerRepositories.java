package ru.agroeco.pulse.videoserver.infrastructure.persistence;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import ru.agroeco.pulse.videoserver.domain.Camera;
import ru.agroeco.pulse.videoserver.domain.Channel;
import ru.agroeco.pulse.videoserver.domain.NormalizedVideoEvent;
import ru.agroeco.pulse.videoserver.domain.RawVideoEvent;
import ru.agroeco.pulse.videoserver.domain.VideoServer;

public interface VideoServerRepository extends JpaRepository<VideoServer, UUID> {
}

public interface CameraRepository extends JpaRepository<Camera, UUID> {
}

public interface ChannelRepository extends JpaRepository<Channel, UUID> {
}

public interface RawVideoEventRepository extends JpaRepository<RawVideoEvent, UUID> {
}

public interface NormalizedVideoEventRepository extends JpaRepository<NormalizedVideoEvent, UUID> {
}
