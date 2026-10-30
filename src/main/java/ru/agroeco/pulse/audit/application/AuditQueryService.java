// audit/application/AuditQueryService.java
package ru.agroeco.pulse.audit.application;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.agroeco.pulse.audit.adapter.http.dto.AuditEventSummary;
import ru.agroeco.pulse.audit.adapter.persistence.AuditEventEntity;
import ru.agroeco.pulse.audit.adapter.persistence.AuditEventRepository;
import ru.agroeco.pulse.platform.api.dto.PageResponse;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AuditQueryService {

    private final AuditEventRepository repository;

    public PageResponse<AuditEventSummary> search(
            Specification<AuditEventEntity> spec, Pageable pageable) {
        Page<AuditEventEntity> page = repository.findAll(spec, pageable);
        return PageResponse.of(page, AuditQueryService::toSummary);
    }

    private static AuditEventSummary toSummary(AuditEventEntity e) {
        return new AuditEventSummary(
                e.getId(),
                e.getOccurredAt(),
                e.getActorId(),
                e.getAction(),
                e.getEntitySchema(),
                e.getEntityTable(),
                e.getEntityId(),
                e.getRequestId(),
                e.getSourceIp() != null ? e.getSourceIp().getHostAddress() : null,
                e.getUserAgent()
        );
    }
}