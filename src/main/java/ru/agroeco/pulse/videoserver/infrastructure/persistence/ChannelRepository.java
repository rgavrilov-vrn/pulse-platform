package ru.agroeco.pulse.videoserver.infrastructure.persistence;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import ru.agroeco.pulse.videoserver.domain.Channel;

public interface ChannelRepository extends JpaRepository<Channel, UUID> {
}
