package ru.agroeco.pulse.incident.adapter.http;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.persistence.criteria.Predicate;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import ru.agroeco.pulse.platform.security.AppPrincipal;
import ru.agroeco.pulse.incident.adapter.http.dto.*;
import ru.agroeco.pulse.incident.application.IncidentService;
import ru.agroeco.pulse.incident.domain.IncidentEntity;
import ru.agroeco.pulse.incident.domain.IncidentSeverity;
import ru.agroeco.pulse.incident.domain.IncidentStatus;
import ru.agroeco.pulse.platform.api.support.QuerySupport;
import ru.agroeco.pulse.platform.domain.exception.BusinessRuleException;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/incidents")
@RequiredArgsConstructor
@Tag(name = "Incidents", description = "Управление инцидентами")
public class IncidentController {

    private static final Set<String> ALLOWED_SORT = Set.of(
            "number", "eventTime", "createdAt", "updatedAt", "status", "severity", "source", "id");
    private static final String DEFAULT_SORT = "eventTime";

    private final IncidentService incidentService;

    // ------------------------------------------------------------------ CREATE
    @PostMapping
    @PreAuthorize("hasAuthority('incident:write')")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Create incident")
    public IncidentDetails create(
            @Valid @RequestBody CreateIncidentRequest req,
            @AuthenticationPrincipal AppPrincipal principal) {
        return incidentService.create(req, principal.id());
    }

    // -------------------------------------------------------------------- LIST
    @GetMapping
    @PreAuthorize("hasAuthority('incident:read')")
    @Operation(summary = "List incidents with filters and pagination",
            description = "Фильтры: status, severity, source (exact), assignedTo, "
                    + "eventFrom/eventTo (ISO-8601, from ≤ to). "
                    + "Sort: " + "number, eventTime, createdAt, updatedAt, status, severity, source, id. "
                    + "Size ограничен 100.")
    public PageResponse<IncidentSummary> list(
            @RequestParam(required = false) IncidentStatus status,
            @RequestParam(required = false) IncidentSeverity severity,
            @RequestParam(required = false) String source,
            @RequestParam(required = false) UUID assignedTo,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant eventFrom,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant eventTo,
            @PageableDefault(
                    size = 20,
                    sort = DEFAULT_SORT,
                    direction = Sort.Direction.DESC)
            Pageable pageable
    ) {
        validateRange(eventFrom, eventTo);

        Pageable safe = QuerySupport.sanitize(
                pageable, ALLOWED_SORT, DEFAULT_SORT, Sort.Direction.DESC);

        Specification<IncidentEntity> spec = buildSpec(
                status, severity, source, assignedTo, eventFrom, eventTo);

        return incidentService.list(spec, safe);
    }

    // ----------------------------------------------------------- CHANGE STATUS
    @PatchMapping("/{id}/status")
    @PreAuthorize("hasAnyAuthority('incident:write', 'incident:review')")
    @Operation(summary = "Change incident status (validated against status_transitions)")
    public IncidentDetails changeStatus(
            @PathVariable UUID id,
            @Valid @RequestBody ChangeStatusRequest req,
            @AuthenticationPrincipal AppPrincipal principal) {
        return incidentService.changeStatus(id, req, principal.id());
    }

    // ------------------------------------------------------------ ADD COMMENT
    @PostMapping("/{id}/comments")
    @PreAuthorize("hasAuthority('incident:write')")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Add comment to incident")
    public CommentView addComment(
            @PathVariable UUID id,
            @Valid @RequestBody AddCommentRequest req,
            @AuthenticationPrincipal AppPrincipal principal) {
        return incidentService.addComment(id, req, principal.id());
    }

    // --------------------------------------------------------------- helpers

    /**
     * Строит Specification по фильтрам. Живёт в контроллере — как в
     * refs/videoserver: сервис получает уже готовый spec и делает
     * findAll + маппинг, не зная деталей фильтрации.
     */
    private Specification<IncidentEntity> buildSpec(
            IncidentStatus status,
            IncidentSeverity severity,
            String source,
            UUID assignedTo,
            Instant eventFrom,
            Instant eventTo
    ) {
        return (root, q, cb) -> {
            List<Predicate> p = new ArrayList<>();

            if (status != null) {
                p.add(cb.equal(root.get("status"), status));
            }
            if (severity != null) {
                p.add(cb.equal(root.get("severity"), severity));
            }
            // isBlank-проверка: раньше ?source= (пустая строка) давал
            // фильтр WHERE source = '', что всегда давало пустой результат.
            if (source != null && !source.isBlank()) {
                p.add(cb.equal(root.get("source"), source));
            }
            if (assignedTo != null) {
                p.add(cb.equal(root.get("assignedTo"), assignedTo));
            }
            if (eventFrom != null) {
                p.add(cb.greaterThanOrEqualTo(root.get("eventTime"), eventFrom));
            }
            if (eventTo != null) {
                p.add(cb.lessThanOrEqualTo(root.get("eventTime"), eventTo));
            }
            return cb.and(p.toArray(new Predicate[0]));
        };
    }

    /** Та же проверка, что в VideoEventQueryController. */
    private void validateRange(Instant from, Instant to) {
        if (from != null && to != null && from.isAfter(to)) {
            throw new BusinessRuleException(
                    "INVALID_DATE_RANGE: 'eventFrom' must be <= 'eventTo' (from=" + from + ", to=" + to + ")");
        }
    }
}