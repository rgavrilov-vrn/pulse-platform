package ru.agroeco.pulse.trassir.application;

import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import ru.agroeco.pulse.platform.api.support.CorrelationIds;
import ru.agroeco.pulse.platform.domain.security.AppPrincipal;
import ru.agroeco.pulse.trassir.adapter.persistence.AdminAuditLogRepository;
import ru.agroeco.pulse.trassir.domain.AdminAuditLog;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class TrassirAdminAuditService {

    private final AdminAuditLogRepository repository;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void recordSuccess(String action, String target, Map<String, Object> details,
                              Integer affectedCount) {
        record(action, target, details, "SUCCESS", null, affectedCount);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void recordFailure(String action, String target, Map<String, Object> details,
                              String errorMessage) {
        record(action, target, details, "FAILED", errorMessage, null);
    }

    private void record(String action, String target, Map<String, Object> details,
                        String status, String errorMessage, Integer affectedCount) {
        try {
            repository.save(AdminAuditLog.builder()
                    .actor(currentActor())
                    .action(action)
                    .target(target)
                    .details(details)
                    .status(status)
                    .errorMessage(errorMessage)
                    .affectedCount(affectedCount)
                    .correlationId(CorrelationIds.current())
                    .build());
        } catch (RuntimeException e) {
            log.error("Failed to write admin audit: action={}, target={}", action, target, e);
        }
    }

    @Transactional(readOnly = true)
    public Page<AdminAuditLog> search(String actor, String action, String status,
                                      Instant from, Instant to, Pageable pageable) {
        return repository.findAll(buildSpec(actor, action, status, from, to), pageable);
    }

    @Transactional(readOnly = true)
    public List<String> distinctActions() { return repository.distinctActions(); }

    @Transactional(readOnly = true)
    public List<String> distinctActors() { return repository.distinctActors(); }

    // ---------------------------------------------------------------- spec

    /**
     * Specification вместо JPQL с ? IS NULL: PostgreSQL не может вывести тип
     * Instant-параметра в "? IS NULL" ($N type inference failure).
     */
    private static Specification<AdminAuditLog> buildSpec(String actor, String action, String status,
                                                          Instant from, Instant to) {
        return (root, query, cb) -> {
            List<Predicate> p = new ArrayList<>();
            if (actor  != null && !actor.isBlank())  p.add(cb.equal(root.get("actor"),  actor));
            if (action != null && !action.isBlank()) p.add(cb.equal(root.get("action"), action));
            if (status != null && !status.isBlank()) p.add(cb.equal(root.get("status"), status));
            if (from != null) p.add(cb.greaterThanOrEqualTo(root.get("createdAt"), from));
            if (to   != null) p.add(cb.lessThanOrEqualTo(root.get("createdAt"), to));
            return cb.and(p.toArray(new Predicate[0]));
        };
    }

    private static String currentActor() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()
                || !(auth.getPrincipal() instanceof AppPrincipal p)) return "system";
        return p.id().toString();
    }
}