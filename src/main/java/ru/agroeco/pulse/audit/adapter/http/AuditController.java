// audit/adapter/http/AuditController.java
package ru.agroeco.pulse.audit.adapter.http;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import ru.agroeco.pulse.audit.adapter.http.dto.AuditEventSummary;
import ru.agroeco.pulse.audit.adapter.persistence.AuditEventEntity;
import ru.agroeco.pulse.audit.application.AuditQueryService;
import ru.agroeco.pulse.platform.api.dto.PageResponse;
import ru.agroeco.pulse.platform.api.support.QuerySupport;
import ru.agroeco.pulse.platform.domain.exception.BusinessRuleException;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/audit")
@RequiredArgsConstructor
@Tag(name = "Audit", description = "Журнал аудита (только чтение)")
public class AuditController {

    private static final Set<String> ALLOWED_SORT = Set.of(
            "occurredAt", "action", "actorId", "entitySchema", "entityTable",
            "entityId", "requestId", "id");
    private static final String DEFAULT_SORT = "occurredAt";

    /** Escape-символ для LIKE; экранируем пользовательские '%' и '_'. */
    private static final char LIKE_ESCAPE = '\\';

    private final AuditQueryService queryService;

    @GetMapping
    @PreAuthorize("hasAuthority('audit:read')")
    @Operation(
            summary = "List audit events with filters, free-text search and pagination",
            description = "Filters: action, entitySchema, entityTable, entityId, actorId, requestId (exact), "
                    + "q (case-insensitive contains over action/requestId/userAgent/entitySchema/entityTable), "
                    + "from/to — occurredAt range (ISO-8601, from ≤ to). "
                    + "Sort: occurredAt, action, actorId, entitySchema, entityTable, entityId, requestId, id. "
                    + "Size ограничен 100.")
    public PageResponse<AuditEventSummary> list(
            @RequestParam(required = false) String action,
            @RequestParam(required = false) String entitySchema,
            @RequestParam(required = false) String entityTable,
            @RequestParam(required = false) UUID entityId,
            @RequestParam(required = false) UUID actorId,
            @RequestParam(required = false) String requestId,
            @RequestParam(required = false) String q,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant to,
            @PageableDefault(size = 20, sort = DEFAULT_SORT, direction = Sort.Direction.DESC)
            Pageable pageable
    ) {
        validateRange(from, to);
        Pageable safe = QuerySupport.sanitize(
                pageable, ALLOWED_SORT, DEFAULT_SORT, Sort.Direction.DESC);
        return queryService.search(
                buildSpec(action, entitySchema, entityTable, entityId, actorId, requestId, q, from, to),
                safe);
    }

    // ------------------------------------------------------------------ helpers

    private Specification<AuditEventEntity> buildSpec(
            String action, String entitySchema, String entityTable,
            UUID entityId, UUID actorId, String requestId, String q,
            Instant from, Instant to) {
        return (root, query, cb) -> {
            List<Predicate> p = new ArrayList<>();
            if (action != null && !action.isBlank())
                p.add(cb.equal(root.get("action"), action));
            if (entitySchema != null && !entitySchema.isBlank())
                p.add(cb.equal(root.get("entitySchema"), entitySchema));
            if (entityTable != null && !entityTable.isBlank())
                p.add(cb.equal(root.get("entityTable"), entityTable));
            if (entityId != null)
                p.add(cb.equal(root.get("entityId"), entityId));
            if (actorId != null)
                p.add(cb.equal(root.get("actorId"), actorId));
            if (requestId != null && !requestId.isBlank())
                p.add(cb.equal(root.get("requestId"), requestId));

            if (q != null && !q.isBlank()) {
                String pattern = toLikePattern(q);
                p.add(cb.or(
                        cb.like(cb.lower(root.get("action")),       pattern, LIKE_ESCAPE),
                        cb.like(cb.lower(root.get("requestId")),    pattern, LIKE_ESCAPE),
                        cb.like(cb.lower(root.get("userAgent")),    pattern, LIKE_ESCAPE),
                        cb.like(cb.lower(root.get("entitySchema")), pattern, LIKE_ESCAPE),
                        cb.like(cb.lower(root.get("entityTable")),  pattern, LIKE_ESCAPE)
                ));
            }

            if (from != null) p.add(cb.greaterThanOrEqualTo(root.get("occurredAt"), from));
            if (to   != null) p.add(cb.lessThanOrEqualTo   (root.get("occurredAt"), to));
            return cb.and(p.toArray(new Predicate[0]));
        };
    }

    private static String toLikePattern(String raw) {
        // Сначала экранируем '\', потом спец-символы LIKE.
        String escaped = raw.toLowerCase(Locale.ROOT)
                .replace("\\", "\\\\")
                .replace("%",  "\\%")
                .replace("_",  "\\_");
        return "%" + escaped + "%";
    }

    private void validateRange(Instant from, Instant to) {
        if (from != null && to != null && from.isAfter(to)) {
            throw new BusinessRuleException(
                    "INVALID_DATE_RANGE: 'from' must be <= 'to' (from=" + from + ", to=" + to + ")");
        }
    }
}