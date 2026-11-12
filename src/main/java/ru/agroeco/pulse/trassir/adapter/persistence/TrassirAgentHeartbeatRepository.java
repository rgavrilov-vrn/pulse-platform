package ru.agroeco.pulse.trassir.adapter.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import ru.agroeco.pulse.trassir.domain.TrassirAgentHeartbeat;

import java.util.UUID;

public interface TrassirAgentHeartbeatRepository extends JpaRepository<TrassirAgentHeartbeat, UUID> {

    @Modifying
    @Query("delete from TrassirAgentHeartbeat heartbeat")
    int deleteAllHeartbeats();
}
