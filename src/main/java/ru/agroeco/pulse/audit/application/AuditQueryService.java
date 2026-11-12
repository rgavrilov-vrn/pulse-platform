package ru.agroeco.pulse.audit.application;

import jakarta.persistence.criteria.Predicate;
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
import ru.agroeco.pulse.platform.api.support.QuerySupport;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AuditQueryService {

    private static final char LIKE_ESCAPE = '\\';

    private final AuditEventRepository repository;

    /**
     * FIX #129: принимает AuditFilter, а не Specification. Persistence-детали
     * (Criteria API, LIKE, escape) остаются внутри сервиса — HTTP-слой
     * не знает про JPA.
     */
    public PageResponse<AuditEventSummary> search(AuditFilter filter, Pageable pageable) {
        Page<AuditEventEntity> page = repository.findAll(buildSpec(filter), pageable);
        return PageResponse.of(page, AuditQueryService::toSummary);
    }

    private static Specification<AuditEventEntity> buildSpec(AuditFilter f) {
        return (root, query, cb) -> {
            List<Predicate> p = new ArrayList<>();
            if (f.action() != null && !f.action().isBlank())
                p.add(cb.equal(root.get("action"), f.action()));
            if (f.entitySchema() != null && !f.entitySchema().isBlank())
                p.add(cb.equal(root.get("entitySchema"), f.entitySchema()));
            if (f.entityTable() != null && !f.entityTable().isBlank())
                p.add(cb.equal(root.get("entityTable"), f.entityTable()));
            if (f.entityId() != null)
                p.add(cb.equal(root.get("entityId"), f.entityId()));
            if (f.actorId() != null)
                p.add(cb.equal(root.get("actorId"), f.actorId()));
            if (f.requestId() != null && !f.requestId().isBlank())
                p.add(cb.equal(root.get("requestId"), f.requestId()));

            if (f.q() != null && !f.q().isBlank()) {
                String pattern = QuerySupport.containsPattern(f.q());
                p.add(cb.or(
                        cb.like(cb.lower(root.get("action")),       pattern, LIKE_ESCAPE),
                        cb.like(cb.lower(root.get("requestId")),    pattern, LIKE_ESCAPE),
                        cb.like(cb.lower(root.get("userAgent")),    pattern, LIKE_ESCAPE),
                        cb.like(cb.lower(root.get("entitySchema")), pattern, LIKE_ESCAPE),
                        cb.like(cb.lower(root.get("entityTable")),  pattern, LIKE_ESCAPE)
                ));
            }
            if (f.from() != null) p.add(cb.greaterThanOrEqualTo(root.get("occurredAt"), f.from()));
            if (f.to() != null)   p.add(cb.lessThanOrEqualTo(root.get("occurredAt"), f.to()));
            return cb.and(p.toArray(new Predicate[0]));
        };
    }

    private static AuditEventSummary toSummary(AuditEventEntity e) {
        return new AuditEventSummary(
                e.getId(), e.getOccurredAt(), e.getActorId(), e.getAction(),
                e.getEntitySchema(), e.getEntityTable(), e.getEntityId(), e.getRequestId(),
                e.getSourceIp() != null ? e.getSourceIp().getHostAddress() : null,
                e.getUserAgent());
    }
}