package ru.agroeco.pulse.platform.adapter.persistence.audit;

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
