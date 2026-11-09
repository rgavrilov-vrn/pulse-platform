package ru.agroeco.pulse.platform.trassir.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.agroeco.pulse.platform.trassir.domain.TrassirAgentHeartbeat;

import java.util.UUID;

public interface TrassirAgentHeartbeatRepository extends JpaRepository<TrassirAgentHeartbeat, UUID> {
}
