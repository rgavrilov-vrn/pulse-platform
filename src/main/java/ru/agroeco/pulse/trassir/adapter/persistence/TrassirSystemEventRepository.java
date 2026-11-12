package ru.agroeco.pulse.trassir.adapter.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import ru.agroeco.pulse.trassir.domain.TrassirSystemEvent;

import java.util.UUID;

public interface TrassirSystemEventRepository extends JpaRepository<TrassirSystemEvent, UUID> {

    @Modifying
    @Query("delete from TrassirSystemEvent event")
    int deleteAllSystemEvents();
}
