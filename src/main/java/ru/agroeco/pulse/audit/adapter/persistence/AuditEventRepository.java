// audit/adapter/persistence/AuditEventRepository.java
package ru.agroeco.pulse.audit.adapter.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface AuditEventRepository
        extends JpaRepository<AuditEventEntity, UUID>,
        JpaSpecificationExecutor<AuditEventEntity> {

    List<AuditEventEntity> findByEntitySchemaAndEntityTableAndEntityId(
            String entitySchema, String entityTable, UUID entityId);

    List<AuditEventEntity> findByOccurredAtAfterOrderByOccurredAtDesc(Instant occurredAfter);
}