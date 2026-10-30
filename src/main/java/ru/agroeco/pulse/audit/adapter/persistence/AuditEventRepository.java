package ru.agroeco.pulse.audit.adapter.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.agroeco.pulse.audit.domain.AuditEventId;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface AuditEventRepository extends JpaRepository<AuditEventEntity, AuditEventId> {

    List<AuditEventEntity> findByEntitySchemaAndEntityTableAndEntityId(
            String entitySchema, String entityTable, UUID entityId);

    List<AuditEventEntity> findByOccurredAtAfterOrderByOccurredAtDesc(Instant occurredAfter);
}