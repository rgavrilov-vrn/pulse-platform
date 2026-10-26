package ru.agroeco.pulse.videoevents.adapter.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface AuditEventRepository extends JpaRepository<AuditEventEntity, Long> {

    List<AuditEventEntity> findByEntitySchemaAndEntityTableAndEntityId(
        String entitySchema,
        String entityTable,
        UUID entityId
    );

    List<AuditEventEntity> findByOccurredAtAfterOrderByOccurredAtDesc(Instant occurredAfter);
}
