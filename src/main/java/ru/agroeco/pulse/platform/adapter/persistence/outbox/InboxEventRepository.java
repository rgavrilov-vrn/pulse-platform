package ru.agroeco.pulse.platform.adapter.persistence.outbox;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.UUID;

public interface InboxEventRepository extends JpaRepository<InboxEventEntity, InboxEventEntity.Key> {
    // Atomic deduplication uses PostgreSQL SQL, executed by JPA in the caller transaction.
    @Modifying
    @Query(value = "INSERT INTO outbox.processed_events (consumer_name, event_id) VALUES (:consumer, :eventId) "
            + "ON CONFLICT DO NOTHING", nativeQuery = true)
    int insertIfAbsent(@Param("consumer") String consumer, @Param("eventId") UUID eventId);
}
