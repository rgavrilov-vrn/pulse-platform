package ru.agroeco.pulse.platform.trassir.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import ru.agroeco.pulse.platform.trassir.domain.TrassirServerState;

import java.util.UUID;

public interface TrassirServerStateRepository extends JpaRepository<TrassirServerState, UUID> {

    @Modifying
    @Query("delete from TrassirServerState state")
    int deleteAllServerStates();
}
