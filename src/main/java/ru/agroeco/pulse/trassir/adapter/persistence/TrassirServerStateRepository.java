package ru.agroeco.pulse.trassir.adapter.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import ru.agroeco.pulse.trassir.domain.TrassirServerState;

import java.util.UUID;

public interface TrassirServerStateRepository extends JpaRepository<TrassirServerState, UUID> {

    @Modifying
    @Query("delete from TrassirServerState state")
    int deleteAllServerStates();
}