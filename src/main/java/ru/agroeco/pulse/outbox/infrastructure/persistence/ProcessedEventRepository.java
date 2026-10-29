package ru.agroeco.pulse.outbox.infrastructure.persistence;

import java.time.Instant;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import ru.agroeco.pulse.outbox.domain.ProcessedEvent;

public interface ProcessedEventRepository
        extends JpaRepository<ProcessedEvent, ProcessedEventId> {

    @Modifying
    @Query(value = "INSERT INTO outbox.processed_events "
            + "(consumer_name, event_id, processed_at) "
            + "VALUES (:consumer, :eventId, :processedAt) "
            + "ON CONFLICT DO NOTHING", nativeQuery = true)
    int insertIfAbsent(
            @Param("consumer") String consumer,
            @Param("eventId") UUID eventId,
            @Param("processedAt") Instant processedAt);
}
