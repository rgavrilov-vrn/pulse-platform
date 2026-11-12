package ru.agroeco.pulse.trassir.adapter.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import ru.agroeco.pulse.trassir.domain.TrassirServerState;

public interface TrassirServerStateRepository extends JpaRepository<TrassirServerState, String> {
    @Modifying
    @Query("delete from TrassirServerState state")
    int deleteAllServerStates();
}
