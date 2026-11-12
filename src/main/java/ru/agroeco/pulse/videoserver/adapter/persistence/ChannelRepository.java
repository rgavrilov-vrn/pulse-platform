package ru.agroeco.pulse.videoserver.adapter.persistence;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import ru.agroeco.pulse.videoserver.domain.Channel;

public interface ChannelRepository extends JpaRepository<Channel, UUID> {
}
