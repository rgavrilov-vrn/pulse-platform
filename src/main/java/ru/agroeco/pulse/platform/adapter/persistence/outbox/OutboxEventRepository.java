package ru.agroeco.pulse.platform.adapter.persistence.outbox;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface OutboxEventRepository extends JpaRepository<OutboxEventEntity, UUID> {
    @Query(value = "SELECT * FROM outbox.outbox_events WHERE status = 'NEW' AND next_attempt_at <= :now "
            + "ORDER BY created_at, id LIMIT :batchSize FOR UPDATE SKIP LOCKED", nativeQuery = true)
    List<OutboxEventEntity> lockPending(@Param("now") Instant now, @Param("batchSize") int batchSize);
}
