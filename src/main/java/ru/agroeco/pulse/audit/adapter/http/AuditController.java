package ru.agroeco.pulse.audit.adapter.http;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import ru.agroeco.pulse.audit.adapter.http.dto.AuditEventSummary;
import ru.agroeco.pulse.audit.application.AuditFilter;
import ru.agroeco.pulse.audit.application.AuditQueryService;
import ru.agroeco.pulse.platform.api.dto.PageResponse;
import ru.agroeco.pulse.platform.api.support.QuerySupport;
import ru.agroeco.pulse.platform.domain.exception.BusinessRuleException;

import java.time.Instant;
import java.util.Set;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/audit")
@RequiredArgsConstructor
@Validated
@Tag(name = "Audit", description = "Журнал аудита (только чтение)")
public class AuditController {

    private static final Set<String> ALLOWED_SORT = Set.of(
            "occurredAt", "action", "actorId", "entitySchema", "entityTable",
            "entityId", "requestId", "id");
    private static final String DEFAULT_SORT = "occurredAt";

    private final AuditQueryService queryService;

    @GetMapping
    @PreAuthorize("hasAuthority('audit:read')")
    @Operation(summary = "List audit events with filters, free-text search and pagination")
    public PageResponse<AuditEventSummary> list(
            @RequestParam(required = false) String action,
            @RequestParam(required = false) String entitySchema,
            @RequestParam(required = false) String entityTable,
            @RequestParam(required = false) UUID entityId,
            @RequestParam(required = false) UUID actorId,
            @RequestParam(required = false) String requestId,
            @RequestParam(required = false) @Size(max = 500) String q,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant to,
            @PageableDefault(size = 20, sort = DEFAULT_SORT, direction = Sort.Direction.DESC)
            Pageable pageable
    ) {
        if (from != null && to != null && from.isAfter(to)) {
            throw new BusinessRuleException(
                    "INVALID_DATE_RANGE: 'from' must be <= 'to' (from=" + from + ", to=" + to + ")");
        }
        Pageable safe = QuerySupport.sanitize(
                pageable, ALLOWED_SORT, DEFAULT_SORT, Sort.Direction.DESC);
        AuditFilter filter = new AuditFilter(
                action, entitySchema, entityTable, entityId, actorId, requestId, q, from, to);
        return queryService.search(filter, safe);
    }
}