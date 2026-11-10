package ru.agroeco.pulse.platform.trassir.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import ru.agroeco.pulse.platform.trassir.domain.TrassirEvent;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface TrassirEventRepository extends JpaRepository<TrassirEvent, UUID> {

    List<TrassirEvent> findTop50ByStatusAndNextRetryAtBeforeOrderByNextRetryAtAsc(
            String status,
            Instant nextRetryAt
    );

    @Modifying
    @Query("delete from TrassirEvent event where event.createdAt < :cutoff and event.status in ('PROCESSED', 'DEAD_LETTER')")
    int deleteFinishedOlderThan(@Param("cutoff") Instant cutoff);

    @Modifying
    @Query("delete from TrassirEvent event")
    int deleteAllEvents();
}
