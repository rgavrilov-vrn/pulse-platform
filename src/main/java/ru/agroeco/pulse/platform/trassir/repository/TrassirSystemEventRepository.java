package ru.agroeco.pulse.platform.trassir.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import ru.agroeco.pulse.platform.trassir.domain.TrassirSystemEvent;

import java.util.UUID;

public interface TrassirSystemEventRepository extends JpaRepository<TrassirSystemEvent, UUID> {

    @Modifying
    @Query("delete from TrassirSystemEvent event")
    int deleteAllSystemEvents();
}
